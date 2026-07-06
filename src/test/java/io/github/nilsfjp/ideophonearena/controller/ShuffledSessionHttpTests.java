package io.github.nilsfjp.ideophonearena.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.nilsfjp.ideophonearena.model.AppUser;
import io.github.nilsfjp.ideophonearena.model.DerivedRound;
import io.github.nilsfjp.ideophonearena.model.GameSession;
import io.github.nilsfjp.ideophonearena.model.PlayerAnswer;
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
 * Seed-derived shuffle over the wire. The session is created through the
 * repository so its seed is known, then the two served practice rounds and all
 * 30 seeded scored rounds are played through the HTTP API. The served order,
 * sides, and target meanings must match the derivation, the stored answers must
 * carry the derived targets, and completion and duplicate semantics must be
 * unchanged.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ShuffledSessionHttpTests {

    private static final long SHUFFLE_SEED = 987654321L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TrialRepository trialRepository;

    @Autowired
    private RoundShuffler roundShuffler;

    @Autowired
    private PlayerAnswerRepository playerAnswerRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void fullSessionWithPracticeFollowsDerivationAndStoresDerivedTargets() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String username = "shuffle_http_" + suffix;
        String token = registerAndGetToken(username);
        AppUser user = appUserRepository.findByUsername(username).orElseThrow();

        // Seed-only fixtures: every session serves the SAME 30 scored trials
        // (ids 1-30) and, with practice, the first 2 practice trials (31, 32).
        List<Trial> scored = trialRepository.findByPracticeFalseOrderByIdAsc();
        List<Trial> practice = trialRepository.findByPracticeTrueOrderByIdAsc();
        List<Trial> servedPractice = practice.subList(0, 2);

        GameSession session = gameSessionRepository.save(new GameSession(
                user, ConditionName.CONDITION_1_SOKUON, 1, true, SHUFFLE_SEED));
        String sessionUuid = session.getSessionUuid();

        List<DerivedRound> derivedPractice = roundShuffler.derivePracticeRounds(SHUFFLE_SEED, servedPractice);
        List<DerivedRound> derivedScored = roundShuffler.deriveScoredRounds(SHUFFLE_SEED, scored);

        for (DerivedRound expected : derivedPractice) {
            String roundJson = getNextRound(token, sessionUuid);
            assertEquals(Boolean.TRUE, JsonPath.read(roundJson, "$.practice"));
            assertServedAsDerived(expected, roundJson);
            String answerJson = submitAnswer(token, sessionUuid, expected.getTrial().getId(),
                    expected.getTarget().getId());
            assertEquals(Boolean.TRUE, JsonPath.read(answerJson, "$.practice"));
            assertEquals(Boolean.TRUE, JsonPath.read(answerJson, "$.correct"));
        }

        long answered = 0;
        for (DerivedRound expected : derivedScored) {
            String roundJson = getNextRound(token, sessionUuid);
            assertEquals(Boolean.FALSE, JsonPath.read(roundJson, "$.practice"));
            assertServedAsDerived(expected, roundJson);

            String answerJson = submitAnswer(token, sessionUuid, expected.getTrial().getId(),
                    expected.getTarget().getId());
            answered++;
            assertEquals(Boolean.TRUE, JsonPath.read(answerJson, "$.correct"));
            assertEquals(expected.getTarget().getId().longValue(),
                    ((Number) JsonPath.read(answerJson, "$.correctIdeophoneId")).longValue());
            assertEquals(answered, ((Number) JsonPath.read(answerJson, "$.totalAnswered")).longValue());
            assertEquals(answered, ((Number) JsonPath.read(answerJson, "$.totalCorrect")).longValue());

            // Duplicate answers stay 409 under the derived flow.
            mockMvc.perform(post("/api/game/sessions/{sessionUuid}/answers", sessionUuid)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"roundId":%d,"selectedIdeophoneId":%d,"responseTimeMs":456}
                                    """.formatted(expected.getTrial().getId(), expected.getTarget().getId())))
                    .andExpect(status().isConflict());
        }

        String completionJson = getNextRound(token, sessionUuid);
        assertEquals(Boolean.TRUE, JsonPath.read(completionJson, "$.completed"));
        assertNotNull(gameSessionRepository.findBySessionUuid(sessionUuid).orElseThrow().getCompletedAt());

        assertStoredTargetsMatchDerivation(session.getId(), derivedScored);
    }

    // The EntityGraph on findBySessionId fetches trial and targetWord, so no
    // transaction is needed here. Answers are matched by trial id because
    // answered_at only has second precision.
    private void assertStoredTargetsMatchDerivation(Long sessionId, List<DerivedRound> derivedScored) {
        List<PlayerAnswer> storedAnswers = playerAnswerRepository.findBySessionId(sessionId);
        assertEquals(derivedScored.size(), storedAnswers.size());
        Map<Long, Long> storedTargetByTrialId = new HashMap<>();
        for (PlayerAnswer answer : storedAnswers) {
            storedTargetByTrialId.put(answer.getTrial().getId(), answer.getTargetWord().getId());
        }
        for (DerivedRound derived : derivedScored) {
            assertEquals(derived.getTarget().getId(), storedTargetByTrialId.get(derived.getTrial().getId()),
                    "stored target must match the derivation for trial " + derived.getTrial().getId());
        }
    }

    private void assertServedAsDerived(DerivedRound expected, String roundJson) {
        assertEquals(expected.getTrial().getId().longValue(),
                ((Number) JsonPath.read(roundJson, "$.roundId")).longValue());
        assertEquals(expected.getTarget().getGloss(), JsonPath.read(roundJson, "$.targetTranslation"));
        assertEquals(expected.getTarget().getGloss(), JsonPath.read(roundJson, "$.translations.target"));
        assertEquals(expected.getOther().getGloss(), JsonPath.read(roundJson, "$.translations.other"));
        assertEquals(expected.isTargetMeaningListedFirst(),
                JsonPath.read(roundJson, "$.targetMeaningListedFirst"),
                "served meaning-line order flag must match the seed derivation");
        assertEquals(expected.getLeft().getId().longValue(),
                ((Number) JsonPath.read(roundJson, "$.left.ideophoneId")).longValue());
        assertEquals(expected.getRight().getId().longValue(),
                ((Number) JsonPath.read(roundJson, "$.right.ideophoneId")).longValue());
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
