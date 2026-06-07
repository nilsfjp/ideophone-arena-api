package io.github.nilsfjp.ideophonearena.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class GameLoopHttpTests {

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

    @Test
    void authenticatedHttpFlowCanStartRoundAndSubmitAnswer() throws Exception {
        ensureDemoRoundExists();

        String suffix = Long.toString(System.nanoTime());
        String username = "loop_http_" + suffix;
        String token = registerAndGetToken(username);

        String sessionJson = mockMvc.perform(post("/api/game/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionName":"CONDITION_1_SOKUON","difficultyLevel":1}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String sessionUuid = JsonPath.read(sessionJson, "$.sessionUuid");

        String roundJson = mockMvc.perform(get("/api/game/sessions/{sessionUuid}/rounds/next", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String targetTranslation = JsonPath.read(roundJson, "$.targetTranslation");
        assertNotNull(targetTranslation);
        assertFalse(targetTranslation.isBlank());
        assertEquals(targetTranslation, JsonPath.read(roundJson, "$.targetTranslation"));
        assertEquals(targetTranslation, JsonPath.read(roundJson, "$.prompt"));
        assertEquals(targetTranslation, JsonPath.read(roundJson, "$.translations.target"));
        assertNotNull(JsonPath.read(roundJson, "$.translations.other"));
        assertEquals(800, ((Number) JsonPath.read(roundJson, "$.timing.fixationMs")).intValue());
        assertFalse(roundJson.contains("\"correctIdeophoneId\""));
        assertFalse(roundJson.contains("\"gloss\""));

        Number roundId = JsonPath.read(roundJson, "$.roundId");
        Number selectedIdeophoneId = JsonPath.read(roundJson, "$.left.ideophoneId");
        String answerJson = mockMvc.perform(post("/api/game/sessions/{sessionUuid}/answers", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundId":%d,"selectedIdeophoneId":%d,"responseTimeMs":456}
                                """.formatted(roundId.longValue(), selectedIdeophoneId.longValue())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertNotNull(JsonPath.read(answerJson, "$.correct"));
        assertEquals(targetTranslation, JsonPath.read(answerJson, "$.targetTranslation"));
        assertEquals(targetTranslation, JsonPath.read(answerJson, "$.prompt"));
        assertTrue(((Number) JsonPath.read(answerJson, "$.totalAnswered")).longValue() >= 1L);
        assertTrue(((Number) JsonPath.read(answerJson, "$.totalCorrect")).longValue() >= 0L);
    }

    @Test
    void startSessionRejectsUnsupportedDifficulty() throws Exception {
        String username = "bad_difficulty_" + System.nanoTime();
        String token = registerAndGetToken(username);

        mockMvc.perform(post("/api/game/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionName":"CONDITION_1_SOKUON","difficultyLevel":2}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startSessionRejectsUnknownConditionName() throws Exception {
        String username = "bad_condition_" + System.nanoTime();
        String token = registerAndGetToken(username);

        mockMvc.perform(post("/api/game/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionName":"UNKNOWN_CONDITION","difficultyLevel":1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nextRoundReturnsExplicitCompletionBodyAfterFinalAnswer() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String username = "complete_http_" + suffix;
        String token = registerAndGetToken(username);
        AppUser user = appUserRepository.findByUsername(username).orElseThrow();
        int isolatedDifficulty = Math.toIntExact(100_000L + (System.nanoTime() % 1_000_000L));

        Ideophone correct = ideophoneRepository.save(new Ideophone(
                "完了左" + suffix.substring(suffix.length() - 6),
                "complete-left-" + suffix,
                "completion target " + suffix,
                "CL" + suffix.substring(suffix.length() - 8),
                "complete-left-" + suffix + ".mp4",
                Modality.AUDITORY
        ));
        Ideophone distractor = ideophoneRepository.save(new Ideophone(
                "完了右" + suffix.substring(suffix.length() - 6),
                "complete-right-" + suffix,
                "completion distractor " + suffix,
                "CR" + suffix.substring(suffix.length() - 8),
                "complete-right-" + suffix + ".mp4",
                Modality.AUDITORY
        ));
        ArenaRound isolatedRound = arenaRoundRepository.save(new ArenaRound(
                "completion target " + suffix,
                correct,
                distractor,
                correct,
                ConditionName.TEXT_ONLY,
                isolatedDifficulty
        ));
        GameSession session = gameSessionRepository.save(new GameSession(
                user,
                ConditionName.TEXT_ONLY,
                isolatedDifficulty
        ));

        String sessionUuid = session.getSessionUuid();
        String roundJson = mockMvc.perform(get("/api/game/sessions/{sessionUuid}/rounds/next", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertEquals(isolatedRound.getId().longValue(), ((Number) JsonPath.read(roundJson, "$.roundId")).longValue());
        assertEquals(Boolean.FALSE, JsonPath.read(roundJson, "$.completed"));

        mockMvc.perform(post("/api/game/sessions/{sessionUuid}/answers", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundId":%d,"selectedIdeophoneId":%d,"responseTimeMs":789}
                                """.formatted(isolatedRound.getId(), correct.getId())))
                .andExpect(status().isOk());

        String completionJson = mockMvc.perform(get("/api/game/sessions/{sessionUuid}/rounds/next", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(Boolean.TRUE, JsonPath.read(completionJson, "$.completed"));
        assertEquals("Game session is complete", JsonPath.read(completionJson, "$.message"));
        assertEquals(sessionUuid, JsonPath.read(completionJson, "$.sessionUuid"));
        assertEquals("TEXT_ONLY", JsonPath.read(completionJson, "$.conditionName"));
        assertEquals(isolatedDifficulty, ((Number) JsonPath.read(completionJson, "$.difficultyLevel")).intValue());
        assertNull(JsonPath.read(completionJson, "$.roundId"));
        GameSession completedSession = gameSessionRepository.findBySessionUuid(sessionUuid).orElseThrow();
        assertNotNull(completedSession.getCompletedAt());
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

    private void ensureDemoRoundExists() {
        if (arenaRoundRepository.countByConditionNameAndDifficultyLevel(ConditionName.CONDITION_1_SOKUON, 1) > 0) {
            return;
        }

        String suffix = Long.toString(System.nanoTime());
        String targetTranslation = "target meaning " + suffix;
        String distractorTranslation = "distractor meaning " + suffix;
        Ideophone correct = ideophoneRepository.save(new Ideophone(
                "テスト左" + suffix.substring(suffix.length() - 6),
                "test-left-" + suffix,
                targetTranslation,
                "TL" + suffix.substring(suffix.length() - 8),
                "test-left-" + suffix + ".mp4",
                Modality.AUDITORY
        ));
        Ideophone distractor = ideophoneRepository.save(new Ideophone(
                "テスト右" + suffix.substring(suffix.length() - 6),
                "test-right-" + suffix,
                distractorTranslation,
                "TR" + suffix.substring(suffix.length() - 8),
                "test-right-" + suffix + ".mp4",
                Modality.AUDITORY
        ));
        arenaRoundRepository.save(new ArenaRound(
                targetTranslation,
                correct,
                distractor,
                correct,
                ConditionName.CONDITION_1_SOKUON,
                1
        ));
    }
}
