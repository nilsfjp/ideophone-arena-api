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
                                {"conditionName":"CONDITION_1_SOKUON"}
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
    void startSessionRequiresConditionName() throws Exception {
        String username = "missing_condition_" + System.nanoTime();
        String token = registerAndGetToken(username);

        mockMvc.perform(post("/api/game/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.conditionName").exists());
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
                                    {"conditionName":"%s"}
                                    """.formatted(conditionName.name())))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            assertEquals(conditionName.name(), JsonPath.read(sessionJson, "$.conditionName"));
            assertEquals("CHOOSING", JsonPath.read(sessionJson, "$.gameMode"));
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
                                    {"conditionName":"%s"}
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
            assertNotNull(JsonPath.read(roundJson, "$.roundId"));
            assertNotNull(JsonPath.read(roundJson, "$.targetTranslation"));
            assertNotNull(JsonPath.read(roundJson, "$.translations.target"));
            assertNotNull(JsonPath.read(roundJson, "$.left.ideophoneId"));
            assertNotNull(JsonPath.read(roundJson, "$.left.kana"));
            assertNotNull(JsonPath.read(roundJson, "$.left.stimulusUrl"));
            assertNotNull(JsonPath.read(roundJson, "$.left.modality"));
            assertNotNull(JsonPath.read(roundJson, "$.right.ideophoneId"));
            assertNotNull(JsonPath.read(roundJson, "$.right.kana"));
            assertNotNull(JsonPath.read(roundJson, "$.right.stimulusUrl"));
            assertNotNull(JsonPath.read(roundJson, "$.right.modality"));
            assertNotNull(JsonPath.read(roundJson, "$.timing.fixationMs"));
            // A7: script_code exposure (canonicalScript) is gone from the choice cards.
            assertFalse(roundJson.contains("canonicalScript"));
        }
    }

    @Test
    void meaningMatchServesFortySevenScoredTrialsExcludingHaptic() {
        // Meaning Match (CHOOSING) serves exactly the 47 A/V/I scored trials (30 thesis +
        // 17 A/V/I expansion). The 4 HAPTIC trials are now live too (Touch floor, NIL-41)
        // but are served only through the Perception Ladder, so the CHOOSING pool -- and its
        // frozen shuffle -- is unchanged. 51 non-practice trials total, 47 of them non-HAPTIC.
        assertEquals(47, trialRepository.findScoredChoosingTrials().size(),
                "Meaning Match must serve exactly the 47 non-HAPTIC scored trials");
        assertEquals(51L, trialRepository.countByPracticeFalse(),
                "51 non-practice trials total: 47 A/V/I + 4 HAPTIC (ladder-only)");
    }

    @Test
    void sessionStartReportsTheScoredRoundTotalSoTheClientNeedNotGuessIt() throws Exception {
        // The client renders "Round n / total" and cannot derive the denominator, so the
        // session response carries it. Since NIL-85 the denominator is the sampled session
        // length (7 auditory + 7 visual + 7 interoceptive), not the size of the scored pool.
        // The literal is the ruling; the inequality proves a session is a strict subset of
        // the pool, so this test fails if sampling is ever silently disabled.
        int expectedScoredRounds = 21;
        assertTrue(expectedScoredRounds < trialRepository.findScoredChoosingTrials().size(),
                "a session must serve a strict subset of the scored pool");
        String token = registerAndGetToken("session_total_" + System.nanoTime());

        mockMvc.perform(post("/api/game/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionName":"CONDITION_1_SOKUON"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalRounds").value(expectedScoredRounds));

        // Practice rounds are not scored, so they never enter the denominator.
        mockMvc.perform(post("/api/game/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionName":"CONDITION_1_SOKUON","includePractice":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.includePractice").value(true))
                .andExpect(jsonPath("$.totalRounds").value(expectedScoredRounds));
    }

    @Test
    void submitAnswerRequiresResponseTimeWithinBounds() throws Exception {
        String username = "answer_validation_" + System.nanoTime();
        String token = registerAndGetToken(username);

        String sessionJson = mockMvc.perform(post("/api/game/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionName":"CONDITION_1_SOKUON"}
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
                                {"conditionName":"UNKNOWN_CONDITION"}
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
                                {"conditionName":"CONDITION_1_SOKUON"}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String sessionUuid = JsonPath.read(sessionJson, "$.sessionUuid");
        int announcedTotal = ((Number) JsonPath.read(sessionJson, "$.totalRounds")).intValue();

        // Play the whole condition-free session (the sampled scored rounds) to completion,
        // answering each round with its own left choice. Remember the first
        // answered round so we can prove a replay is rejected after completion.
        Long firstRoundId = null;
        Long firstSelectedId = null;
        String completionJson = null;
        int answeredRounds = 0;
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
            answeredRounds++;
        }

        assertNotNull(completionJson, "session must reach an explicit completion body");
        assertNotNull(firstRoundId, "at least one scored round must have been served");

        // The denominator announced at session start must be the number of scored rounds
        // the session really served (NIL-89's relationship, now also guarding the NIL-85
        // sample: a sample that served a different count than it announced would freeze the
        // client's progress bar exactly as the old hardcoded total did).
        assertEquals(announcedTotal, answeredRounds,
                "startSession's totalRounds must equal the scored rounds actually served");

        assertEquals(Boolean.TRUE, JsonPath.read(completionJson, "$.completed"));
        assertEquals("Game session is complete", JsonPath.read(completionJson, "$.message"));
        assertEquals(sessionUuid, JsonPath.read(completionJson, "$.sessionUuid"));
        assertEquals("CONDITION_1_SOKUON", JsonPath.read(completionJson, "$.conditionName"));
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
