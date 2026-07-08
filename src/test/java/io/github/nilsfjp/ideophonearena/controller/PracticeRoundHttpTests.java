package io.github.nilsfjp.ideophonearena.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.nilsfjp.ideophonearena.model.AppUser;
import io.github.nilsfjp.ideophonearena.model.DerivedRound;
import io.github.nilsfjp.ideophonearena.model.GameSession;
import io.github.nilsfjp.ideophonearena.model.Trial;
import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;
import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import io.github.nilsfjp.ideophonearena.repository.GameSessionRepository;
import io.github.nilsfjp.ideophonearena.repository.PlayerAnswerRepository;
import io.github.nilsfjp.ideophonearena.repository.TrialRepository;
import io.github.nilsfjp.ideophonearena.service.RoundShuffler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Practice-round flow against the regenerated WORD-grain seed. The game is
 * condition-free, so every session serves the same 47 scored trials (30 thesis +
 * 17 A/V/I expansion) and, with the practice flag, the first two seeded practice
 * trials (31=p0 auditory, 32=p1 visual) first. Sessions are created through the
 * repository with a known seed so the derived order is deterministic; no Trial
 * or Word fixtures are ever created (every trial is served by every session and
 * needs its seeded presentations).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PracticeRoundHttpTests {

    private static final long KNOWN_SEED = 424242L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private PlayerAnswerRepository playerAnswerRepository;

    @Autowired
    private TrialRepository trialRepository;

    @Autowired
    private RoundShuffler roundShuffler;

    @Test
    void practiceSessionServesTwoPracticeRoundsThenScoredRoundsWithoutPersistingPracticeAnswers() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String username = "practice_http_" + suffix;
        String token = registerAndGetToken(username);
        AppUser user = appUserRepository.findByUsername(username).orElseThrow();

        GameSession session = gameSessionRepository.save(new GameSession(
                user, ConditionName.CONDITION_1_SOKUON, true, KNOWN_SEED));
        String sessionUuid = session.getSessionUuid();

        List<Trial> scored = trialRepository.findScoredChoosingTrials();
        List<Trial> practice = trialRepository.findByPracticeTrueOrderByIdAsc();
        List<DerivedRound> derivedPractice = roundShuffler.derivePracticeRounds(KNOWN_SEED, practice.subList(0, 2));
        List<DerivedRound> derivedScored = roundShuffler.deriveScoredRounds(KNOWN_SEED, scored);

        // The two served practice rounds: feedback comes back but the scored
        // totals stay zero and no PlayerAnswer row is written, and the session
        // never completes off a practice answer.
        for (int i = 0; i < 2; i++) {
            DerivedRound expected = derivedPractice.get(i);
            String roundJson = getNextRound(token, sessionUuid);
            assertEquals(expected.getTrial().getId().longValue(),
                    ((Number) JsonPath.read(roundJson, "$.roundId")).longValue());
            assertEquals(Boolean.TRUE, JsonPath.read(roundJson, "$.practice"));
            assertEquals(Boolean.FALSE, JsonPath.read(roundJson, "$.completed"));

            String answerJson = submitAnswer(token, sessionUuid, expected.getTrial().getId(),
                    expected.getTarget().getId());
            assertEquals(Boolean.TRUE, JsonPath.read(answerJson, "$.practice"));
            assertEquals(0, ((Number) JsonPath.read(answerJson, "$.totalAnswered")).intValue());
            assertEquals(0, ((Number) JsonPath.read(answerJson, "$.totalCorrect")).intValue());
            assertEquals(0L, playerAnswerRepository.countBySessionId(session.getId()),
                    "practice answers must not create PlayerAnswer rows");
            assertNull(gameSessionRepository.findBySessionUuid(sessionUuid).orElseThrow().getCompletedAt(),
                    "practice answers must not complete the session");
        }

        // All scored rounds follow; answering them counts and the session
        // completes on the final scored answer even though practice ran first.
        Map<Long, Long> targetByTrialId = new HashMap<>();
        for (DerivedRound derived : derivedScored) {
            targetByTrialId.put(derived.getTrial().getId(), derived.getTarget().getId());
        }
        for (int i = 0; i < scored.size(); i++) {
            String roundJson = getNextRound(token, sessionUuid);
            assertEquals(Boolean.FALSE, JsonPath.read(roundJson, "$.practice"));
            long roundId = ((Number) JsonPath.read(roundJson, "$.roundId")).longValue();
            submitAnswer(token, sessionUuid, roundId, targetByTrialId.get(roundId));
        }

        assertEquals((long) scored.size(), playerAnswerRepository.countBySessionId(session.getId()),
                "exactly the scored answers must be persisted");
        assertNotNull(gameSessionRepository.findBySessionUuid(sessionUuid).orElseThrow().getCompletedAt(),
                "the scored answers must complete the session regardless of practice rounds");
    }

    @Test
    void sessionWithoutPracticeFlagServesScoredRoundsDirectly() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String username = "no_practice_http_" + suffix;
        String token = registerAndGetToken(username);
        AppUser user = appUserRepository.findByUsername(username).orElseThrow();

        GameSession session = gameSessionRepository.save(new GameSession(
                user, ConditionName.CONDITION_1_SOKUON, false, KNOWN_SEED));

        List<Trial> scored = trialRepository.findScoredChoosingTrials();
        DerivedRound firstScored = roundShuffler.deriveScoredRounds(KNOWN_SEED, scored).get(0);

        String roundJson = getNextRound(token, session.getSessionUuid());
        assertEquals(firstScored.getTrial().getId().longValue(),
                ((Number) JsonPath.read(roundJson, "$.roundId")).longValue());
        assertEquals(Boolean.FALSE, JsonPath.read(roundJson, "$.practice"));
    }

    @Test
    void practiceAnswersMustFollowServingOrder() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String username = "practice_order_" + suffix;
        String token = registerAndGetToken(username);
        AppUser user = appUserRepository.findByUsername(username).orElseThrow();

        GameSession session = gameSessionRepository.save(new GameSession(
                user, ConditionName.CONDITION_1_SOKUON, true, KNOWN_SEED));
        String sessionUuid = session.getSessionUuid();

        List<Trial> practice = trialRepository.findByPracticeTrueOrderByIdAsc();
        List<DerivedRound> derivedPractice = roundShuffler.derivePracticeRounds(KNOWN_SEED, practice.subList(0, 2));
        long firstPracticeTrialId = derivedPractice.get(0).getTrial().getId();
        long secondPracticeTrialId = derivedPractice.get(1).getTrial().getId();

        // Second practice round before the first: 400.
        mockMvc.perform(post("/api/game/sessions/{sessionUuid}/answers", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundId":%d,"selectedIdeophoneId":%d,"responseTimeMs":500}
                                """.formatted(secondPracticeTrialId,
                                derivedPractice.get(1).getTarget().getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Practice rounds must be answered in order"));

        submitAnswer(token, sessionUuid, firstPracticeTrialId, derivedPractice.get(0).getTarget().getId());

        // Repeating the first practice round after advancing: 409.
        mockMvc.perform(post("/api/game/sessions/{sessionUuid}/answers", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundId":%d,"selectedIdeophoneId":%d,"responseTimeMs":500}
                                """.formatted(firstPracticeTrialId,
                                derivedPractice.get(0).getTarget().getId())))
                .andExpect(status().isConflict());
    }

    @Test
    void startSessionAcceptsIncludePracticeFlagAndEchoesIt() throws Exception {
        String username = "practice_start_" + System.nanoTime();
        String token = registerAndGetToken(username);

        String withFlagJson = mockMvc.perform(post("/api/game/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionName":"CONDITION_1_SOKUON","difficultyLevel":1,"includePractice":true}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertEquals(Boolean.TRUE, JsonPath.read(withFlagJson, "$.includePractice"));

        String withoutFlagJson = mockMvc.perform(post("/api/game/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionName":"CONDITION_1_SOKUON","difficultyLevel":1}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertEquals(Boolean.FALSE, JsonPath.read(withoutFlagJson, "$.includePractice"));
    }

    @Test
    void seededPracticeSessionServesPracticeRoundFirst() throws Exception {
        String username = "practice_seeded_" + System.nanoTime();
        String token = registerAndGetToken(username);

        String sessionJson = mockMvc.perform(post("/api/game/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionName":"CONDITION_1_SOKUON","difficultyLevel":1,"includePractice":true}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String sessionUuid = JsonPath.read(sessionJson, "$.sessionUuid");

        String roundJson = getNextRound(token, sessionUuid);
        assertEquals(Boolean.TRUE, JsonPath.read(roundJson, "$.practice"));
        String stimulusUrl = JsonPath.read(roundJson, "$.left.stimulusUrl");
        assertNotNull(stimulusUrl);
        assertNotEquals(-1, stimulusUrl.indexOf("/stimuli/audio/p"),
                "seeded practice rounds must reference p-prefix audio: " + stimulusUrl);
    }

    private String registerAndGetToken(String username) throws Exception {
        String authJson = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s@example.test","password":"password123"}
                                """.formatted(username, username)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(authJson, "$.token");
    }

    private String getNextRound(String token, String sessionUuid) throws Exception {
        return mockMvc.perform(get("/api/game/sessions/{sessionUuid}/rounds/next", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String submitAnswer(String token, String sessionUuid, Long roundId, Long selectedIdeophoneId)
            throws Exception {
        return mockMvc.perform(post("/api/game/sessions/{sessionUuid}/answers", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundId":%d,"selectedIdeophoneId":%d,"responseTimeMs":456}
                                """.formatted(roundId, selectedIdeophoneId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
