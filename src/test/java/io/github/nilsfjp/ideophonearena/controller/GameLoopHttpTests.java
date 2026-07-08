package io.github.nilsfjp.ideophonearena.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;
import io.github.nilsfjp.ideophonearena.repository.TrialRepository;
import java.util.List;
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

    private static final List<ConditionName> SUPPORTED_SOKUON_CONDITIONS = List.of(
            ConditionName.CONDITION_1_SOKUON,
            ConditionName.CONDITION_2_SOKUON,
            ConditionName.CONDITION_3_SOKUON
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TrialRepository trialRepository;

    @Test
    void authenticatedHttpFlowCanStartRoundAndSubmitAnswer() throws Exception {
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only difficulty level 1 is supported for the current demo"));
    }

    @Test
    void startSessionRequiresConditionName() throws Exception {
        String username = "missing_condition_" + System.nanoTime();
        String token = registerAndGetToken(username);

        mockMvc.perform(post("/api/game/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"difficultyLevel":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.conditionName").exists());
    }

    @Test
    void startSessionRequiresDifficultyLevel() throws Exception {
        String username = "missing_difficulty_" + System.nanoTime();
        String token = registerAndGetToken(username);

        mockMvc.perform(post("/api/game/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionName":"CONDITION_1_SOKUON"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.difficultyLevel").exists());
    }

    @Test
    void startSessionRejectsTextOnlyCondition() throws Exception {
        String username = "text_only_condition_" + System.nanoTime();
        String token = registerAndGetToken(username);

        mockMvc.perform(post("/api/game/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionName":"TEXT_ONLY","difficultyLevel":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Unsupported conditionName: TEXT_ONLY. Supported values are CONDITION_1_SOKUON, CONDITION_2_SOKUON, CONDITION_3_SOKUON"
                ));
    }

    @Test
    void startSessionCreatesSessionsForSupportedSokuonConditions() throws Exception {
        String username = "supported_conditions_" + System.nanoTime();
        String token = registerAndGetToken(username);

        for (ConditionName conditionName : SUPPORTED_SOKUON_CONDITIONS) {
            String sessionJson = mockMvc.perform(post("/api/game/sessions")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"conditionName":"%s","difficultyLevel":1}
                                    """.formatted(conditionName.name())))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            assertEquals(conditionName.name(), JsonPath.read(sessionJson, "$.conditionName"));
            assertEquals(1, ((Number) JsonPath.read(sessionJson, "$.difficultyLevel")).intValue());
            assertNotNull(JsonPath.read(sessionJson, "$.sessionUuid"));
        }
    }

    @Test
    void supportedSokuonConditionsCanFetchRenderableFirstRound() throws Exception {
        String username = "supported_rounds_" + System.nanoTime();
        String token = registerAndGetToken(username);

        for (ConditionName conditionName : SUPPORTED_SOKUON_CONDITIONS) {
            String sessionJson = mockMvc.perform(post("/api/game/sessions")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"conditionName":"%s","difficultyLevel":1}
                                    """.formatted(conditionName.name())))
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

            assertEquals(Boolean.FALSE, JsonPath.read(roundJson, "$.completed"));
            assertEquals(conditionName.name(), JsonPath.read(roundJson, "$.conditionName"));
            assertEquals(1, ((Number) JsonPath.read(roundJson, "$.difficultyLevel")).intValue());
            assertNotNull(JsonPath.read(roundJson, "$.roundId"));
            assertNotNull(JsonPath.read(roundJson, "$.targetTranslation"));
            assertNotNull(JsonPath.read(roundJson, "$.translations.target"));
            assertNotNull(JsonPath.read(roundJson, "$.left.ideophoneId"));
            assertNotNull(JsonPath.read(roundJson, "$.left.kana"));
            assertNotNull(JsonPath.read(roundJson, "$.left.stimulusUrl"));
            assertNotNull(JsonPath.read(roundJson, "$.left.modality"));
            assertNotNull(JsonPath.read(roundJson, "$.left.canonicalScript"));
            assertNotNull(JsonPath.read(roundJson, "$.right.ideophoneId"));
            assertNotNull(JsonPath.read(roundJson, "$.right.kana"));
            assertNotNull(JsonPath.read(roundJson, "$.right.stimulusUrl"));
            assertNotNull(JsonPath.read(roundJson, "$.right.modality"));
            assertNotNull(JsonPath.read(roundJson, "$.right.canonicalScript"));
            assertNotNull(JsonPath.read(roundJson, "$.timing.fixationMs"));
        }
    }

    @Test
    void seedDataContainsFortySevenScoredConditionFreeTrials() {
        assertEquals(47L, trialRepository.countByPracticeFalse(),
                "seed must expose exactly 47 scored (condition-free) trials served to every session "
                        + "(30 thesis + 17 A/V/I expansion; the 4 HAPTIC expansion pairs stay dark)");
    }

    @Test
    void submitAnswerRequiresResponseTimeWithinBounds() throws Exception {
        String username = "answer_validation_" + System.nanoTime();
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

        mockMvc.perform(post("/api/game/sessions/{sessionUuid}/answers", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundId":1,"selectedIdeophoneId":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.responseTimeMs").exists());

        mockMvc.perform(post("/api/game/sessions/{sessionUuid}/answers", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundId":1,"selectedIdeophoneId":1,"responseTimeMs":600001}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.responseTimeMs").exists());
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

        // Play the whole condition-free session (47 scored rounds) to completion,
        // answering each round with its own left choice. Remember the first
        // answered round so we can prove a replay is rejected after completion.
        Long firstRoundId = null;
        Long firstSelectedId = null;
        String completionJson = null;
        for (int i = 0; i < 60; i++) {
            String roundJson = mockMvc.perform(get("/api/game/sessions/{sessionUuid}/rounds/next", sessionUuid)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            if (Boolean.TRUE.equals(JsonPath.read(roundJson, "$.completed"))) {
                completionJson = roundJson;
                break;
            }

            long roundId = ((Number) JsonPath.read(roundJson, "$.roundId")).longValue();
            long selectedId = ((Number) JsonPath.read(roundJson, "$.left.ideophoneId")).longValue();
            if (firstRoundId == null) {
                firstRoundId = roundId;
                firstSelectedId = selectedId;
            }

            mockMvc.perform(post("/api/game/sessions/{sessionUuid}/answers", sessionUuid)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"roundId":%d,"selectedIdeophoneId":%d,"responseTimeMs":456}
                                    """.formatted(roundId, selectedId)))
                    .andExpect(status().isOk());
        }

        assertNotNull(completionJson, "session must reach an explicit completion body");
        assertNotNull(firstRoundId, "at least one scored round must have been served");

        assertEquals(Boolean.TRUE, JsonPath.read(completionJson, "$.completed"));
        assertEquals("Game session is complete", JsonPath.read(completionJson, "$.message"));
        assertEquals(sessionUuid, JsonPath.read(completionJson, "$.sessionUuid"));
        assertEquals("CONDITION_1_SOKUON", JsonPath.read(completionJson, "$.conditionName"));
        assertEquals(1, ((Number) JsonPath.read(completionJson, "$.difficultyLevel")).intValue());
        assertNull(JsonPath.read(completionJson, "$.roundId"));

        // Re-answering an already-answered round is a conflict.
        mockMvc.perform(post("/api/game/sessions/{sessionUuid}/answers", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundId":%d,"selectedIdeophoneId":%d,"responseTimeMs":456}
                                """.formatted(firstRoundId, firstSelectedId)))
                .andExpect(status().isConflict());
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
}
