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
import io.github.nilsfjp.ideophonearena.model.ArenaRound;
import io.github.nilsfjp.ideophonearena.model.GameSession;
import io.github.nilsfjp.ideophonearena.model.Ideophone;
import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import io.github.nilsfjp.ideophonearena.repository.ArenaRoundRepository;
import io.github.nilsfjp.ideophonearena.repository.GameSessionRepository;
import io.github.nilsfjp.ideophonearena.repository.IdeophoneRepository;
import io.github.nilsfjp.ideophonearena.repository.PlayerAnswerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Practice-round flow against an isolated fixture: two practice rounds plus a
 * single scored round under a unique difficulty level, so the seeded data and
 * other tests cannot interfere. Sessions are created through the repository
 * because the public API locks difficulty to 1.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PracticeRoundHttpTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdeophoneRepository ideophoneRepository;

    @Autowired
    private ArenaRoundRepository arenaRoundRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private PlayerAnswerRepository playerAnswerRepository;

    private record PracticeFixture(ArenaRound firstPractice, ArenaRound secondPractice, ArenaRound scoredRound,
            int difficulty) {
    }

    @Test
    void practiceSessionServesTwoPracticeRoundsThenScoredRoundsWithoutPersistingPracticeAnswers() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String username = "practice_http_" + suffix;
        String token = registerAndGetToken(username);
        AppUser user = appUserRepository.findByUsername(username).orElseThrow();
        PracticeFixture fixture = createFixture(suffix);

        GameSession session = gameSessionRepository.save(new GameSession(
                user, ConditionName.TEXT_ONLY, fixture.difficulty(), true));
        String sessionUuid = session.getSessionUuid();

        // First practice round, answered correctly: feedback comes back but the
        // scored totals stay zero and no PlayerAnswer row is written.
        String firstRoundJson = getNextRound(token, sessionUuid);
        assertEquals(fixture.firstPractice().getId().longValue(),
                ((Number) JsonPath.read(firstRoundJson, "$.roundId")).longValue());
        assertEquals(Boolean.TRUE, JsonPath.read(firstRoundJson, "$.practice"));
        assertEquals(Boolean.FALSE, JsonPath.read(firstRoundJson, "$.completed"));

        String firstAnswerJson = submitAnswer(token, sessionUuid, fixture.firstPractice().getId(),
                fixture.firstPractice().getCorrectIdeophone().getId());
        assertEquals(Boolean.TRUE, JsonPath.read(firstAnswerJson, "$.practice"));
        assertEquals(Boolean.TRUE, JsonPath.read(firstAnswerJson, "$.correct"));
        assertEquals(0, ((Number) JsonPath.read(firstAnswerJson, "$.totalAnswered")).intValue());
        assertEquals(0, ((Number) JsonPath.read(firstAnswerJson, "$.totalCorrect")).intValue());
        assertEquals(0L, playerAnswerRepository.countBySessionId(session.getId()),
                "practice answers must not create PlayerAnswer rows");

        // Second practice round, answered incorrectly: feedback says wrong,
        // totals still untouched.
        String secondRoundJson = getNextRound(token, sessionUuid);
        assertEquals(fixture.secondPractice().getId().longValue(),
                ((Number) JsonPath.read(secondRoundJson, "$.roundId")).longValue());
        assertEquals(Boolean.TRUE, JsonPath.read(secondRoundJson, "$.practice"));

        Long wrongChoice = fixture.secondPractice().getCorrectIdeophone().getId()
                        .equals(fixture.secondPractice().getLeftIdeophone().getId())
                ? fixture.secondPractice().getRightIdeophone().getId()
                : fixture.secondPractice().getLeftIdeophone().getId();
        String secondAnswerJson = submitAnswer(token, sessionUuid, fixture.secondPractice().getId(), wrongChoice);
        assertEquals(Boolean.TRUE, JsonPath.read(secondAnswerJson, "$.practice"));
        assertEquals(Boolean.FALSE, JsonPath.read(secondAnswerJson, "$.correct"));
        assertEquals(0, ((Number) JsonPath.read(secondAnswerJson, "$.totalAnswered")).intValue());
        assertEquals(0L, playerAnswerRepository.countBySessionId(session.getId()));
        assertNull(gameSessionRepository.findBySessionUuid(sessionUuid).orElseThrow().getCompletedAt(),
                "practice answers must not complete the session");

        // Scored round follows; answering it counts and completes the
        // single-round session even though practice rounds were served first.
        String scoredRoundJson = getNextRound(token, sessionUuid);
        assertEquals(fixture.scoredRound().getId().longValue(),
                ((Number) JsonPath.read(scoredRoundJson, "$.roundId")).longValue());
        assertEquals(Boolean.FALSE, JsonPath.read(scoredRoundJson, "$.practice"));

        String scoredAnswerJson = submitAnswer(token, sessionUuid, fixture.scoredRound().getId(),
                fixture.scoredRound().getCorrectIdeophone().getId());
        assertEquals(Boolean.FALSE, JsonPath.read(scoredAnswerJson, "$.practice"));
        assertEquals(1, ((Number) JsonPath.read(scoredAnswerJson, "$.totalAnswered")).intValue());
        assertEquals(1, ((Number) JsonPath.read(scoredAnswerJson, "$.totalCorrect")).intValue());
        assertNotNull(gameSessionRepository.findBySessionUuid(sessionUuid).orElseThrow().getCompletedAt(),
                "the scored answer must complete the session regardless of practice rounds");
    }

    @Test
    void sessionWithoutPracticeFlagServesScoredRoundsDirectly() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String username = "no_practice_http_" + suffix;
        String token = registerAndGetToken(username);
        AppUser user = appUserRepository.findByUsername(username).orElseThrow();
        PracticeFixture fixture = createFixture(suffix);

        GameSession session = gameSessionRepository.save(new GameSession(
                user, ConditionName.TEXT_ONLY, fixture.difficulty()));

        String roundJson = getNextRound(token, session.getSessionUuid());
        assertEquals(fixture.scoredRound().getId().longValue(),
                ((Number) JsonPath.read(roundJson, "$.roundId")).longValue());
        assertEquals(Boolean.FALSE, JsonPath.read(roundJson, "$.practice"));
    }

    @Test
    void practiceAnswersMustFollowServingOrder() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String username = "practice_order_" + suffix;
        String token = registerAndGetToken(username);
        AppUser user = appUserRepository.findByUsername(username).orElseThrow();
        PracticeFixture fixture = createFixture(suffix);

        GameSession session = gameSessionRepository.save(new GameSession(
                user, ConditionName.TEXT_ONLY, fixture.difficulty(), true));
        String sessionUuid = session.getSessionUuid();

        // Second practice round before the first: 400.
        mockMvc.perform(post("/api/game/sessions/{sessionUuid}/answers", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundId":%d,"selectedIdeophoneId":%d,"responseTimeMs":500}
                                """.formatted(fixture.secondPractice().getId(),
                                fixture.secondPractice().getCorrectIdeophone().getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Practice rounds must be answered in order"));

        submitAnswer(token, sessionUuid, fixture.firstPractice().getId(),
                fixture.firstPractice().getCorrectIdeophone().getId());

        // Repeating the first practice round after advancing: 409.
        mockMvc.perform(post("/api/game/sessions/{sessionUuid}/answers", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundId":%d,"selectedIdeophoneId":%d,"responseTimeMs":500}
                                """.formatted(fixture.firstPractice().getId(),
                                fixture.firstPractice().getCorrectIdeophone().getId())))
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

    private PracticeFixture createFixture(String suffix) {
        int difficulty = Math.toIntExact(200_000L + (System.nanoTime() % 1_000_000L));
        ArenaRound firstPractice = practiceRound("p1", suffix, difficulty, true);
        ArenaRound secondPractice = practiceRound("p2", suffix, difficulty, true);
        ArenaRound scoredRound = practiceRound("sr", suffix, difficulty, false);
        return new PracticeFixture(firstPractice, secondPractice, scoredRound, difficulty);
    }

    private ArenaRound practiceRound(String tag, String suffix, int difficulty, boolean practice) {
        String prompt = tag + " target " + suffix;
        Ideophone correct = ideophone(tag + "l", suffix, prompt);
        Ideophone distractor = ideophone(tag + "r", suffix, tag + " distractor " + suffix);
        return arenaRoundRepository.save(new ArenaRound(
                prompt,
                correct,
                distractor,
                correct,
                ConditionName.TEXT_ONLY,
                difficulty,
                practice
        ));
    }

    private Ideophone ideophone(String tag, String suffix, String gloss) {
        String kana = tag + suffix.substring(suffix.length() - 6);
        return ideophoneRepository.save(new Ideophone(
                kana,
                kana,
                kana,
                tag + "-" + suffix,
                gloss,
                tag.toUpperCase() + suffix.substring(suffix.length() - 8),
                tag + "-" + suffix + ".m4a",
                Modality.AUDITORY
        ));
    }
}
