package io.github.nilsfjp.ideophonearena.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
class LadderHttpTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ladderFloorsEndpointReturnsFourFloorsInHierarchyOrder() throws Exception {
        String token = registerAndGetToken("ladder_floors_" + System.nanoTime());

        String floorsJson = getFloors(token);

        assertEquals(4, ((List<?>) JsonPath.read(floorsJson, "$.floors")).size());
        assertEquals("AUDITORY", JsonPath.read(floorsJson, "$.floors[0].modality"));
        assertEquals("VISUAL", JsonPath.read(floorsJson, "$.floors[1].modality"));
        assertEquals("HAPTIC", JsonPath.read(floorsJson, "$.floors[2].modality"));
        assertEquals("INTEROCEPTIVE", JsonPath.read(floorsJson, "$.floors[3].modality"));

        // Sound floor: within-floor order is thesis-facts §8, final pair (a5) last.
        assertEquals(10, ((Number) JsonPath.read(floorsJson, "$.floors[0].pairCount")).intValue());
        assertEquals("a9", JsonPath.read(floorsJson, "$.floors[0].pairs[0].pairCode"));
        assertEquals("a5", JsonPath.read(floorsJson, "$.floors[0].finalRungPairCode"));
        assertEquals(Boolean.TRUE, JsonPath.read(floorsJson, "$.floors[0].pairs[9].finalRung"));
        assertEquals(Boolean.FALSE, JsonPath.read(floorsJson, "$.floors[0].pairs[0].finalRung"));

        // Touch floor is a 4-pair floor (NIL-41 go-live), final rung exp-h6.
        assertEquals(4, ((Number) JsonPath.read(floorsJson, "$.floors[2].pairCount")).intValue());
        assertEquals("exp-h6", JsonPath.read(floorsJson, "$.floors[2].finalRungPairCode"));

        // A fresh player has cleared nothing; no per-pair difficulty numbers leak (V5).
        assertEquals(Boolean.FALSE, JsonPath.read(floorsJson, "$.floors[0].cleared"));
        assertTrue(floorsJson.contains("pairCode"));
        assertTrue(!floorsJson.contains("thesisAccuracy") && !floorsJson.contains("difficulty"));
    }

    @Test
    void ladderFloorSessionServesTheFloorToCompletionAndMarksItCleared() throws Exception {
        String token = registerAndGetToken("ladder_walk_" + System.nanoTime());

        String sessionJson = mockMvc.perform(post("/api/game/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionName":"CONDITION_1_SOKUON","gameMode":"LADDER","floor":"HAPTIC"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertEquals("LADDER", JsonPath.read(sessionJson, "$.gameMode"));
        assertEquals("HAPTIC", JsonPath.read(sessionJson, "$.floor"));
        String sessionUuid = JsonPath.read(sessionJson, "$.sessionUuid");
        // A ladder session's scored total is its floor's pair count, not the CHOOSING pool.
        int announcedTotalRounds = ((Number) JsonPath.read(sessionJson, "$.totalRounds")).intValue();

        int served = 0;
        String completionJson = null;
        for (int i = 0; i < 10; i++) {
            String roundJson = mockMvc.perform(get("/api/game/sessions/{u}/rounds/next", sessionUuid)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            if (Boolean.TRUE.equals(JsonPath.read(roundJson, "$.completed"))) {
                completionJson = roundJson;
                break;
            }
            assertEquals("HAPTIC", JsonPath.read(roundJson, "$.left.modality"), "the Touch floor serves HAPTIC pairs");
            long roundId = ((Number) JsonPath.read(roundJson, "$.roundId")).longValue();
            long selectedId = ((Number) JsonPath.read(roundJson, "$.left.ideophoneId")).longValue();
            mockMvc.perform(post("/api/game/sessions/{u}/answers", sessionUuid)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"roundId":%d,"selectedIdeophoneId":%d,"responseTimeMs":456}
                                    """.formatted(roundId, selectedId)))
                    .andExpect(status().isOk());
            served++;
        }

        assertNotNull(completionJson, "the ladder floor must reach completion");
        assertEquals(4, served, "the Touch floor serves exactly its 4 pairs");
        assertEquals(served, announcedTotalRounds,
                "the session's announced totalRounds must equal the rounds it actually serves");

        // The floor now reports cleared with the caller's best score.
        String floorsJson = getFloors(token);
        assertEquals(Boolean.TRUE, JsonPath.read(floorsJson, "$.floors[2].cleared"));
        assertEquals(4, ((Number) JsonPath.read(floorsJson, "$.floors[2].bestAnswered")).intValue());
    }

    @Test
    void completedLadderSessionDoesNotEnterTheMeaningMatchLeaderboard() throws Exception {
        String token = registerAndGetToken("ladder_no_board_" + System.nanoTime());
        long before = leaderboardTotalElements(token);

        // Complete a whole Touch-floor ladder session.
        String sessionUuid = JsonPath.read(mockMvc.perform(post("/api/game/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionName":"CONDITION_1_SOKUON","gameMode":"LADDER","floor":"HAPTIC"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.sessionUuid");
        for (int i = 0; i < 10; i++) {
            String roundJson = mockMvc.perform(get("/api/game/sessions/{u}/rounds/next", sessionUuid)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            if (Boolean.TRUE.equals(JsonPath.read(roundJson, "$.completed"))) {
                break;
            }
            long roundId = ((Number) JsonPath.read(roundJson, "$.roundId")).longValue();
            long selectedId = ((Number) JsonPath.read(roundJson, "$.left.ideophoneId")).longValue();
            mockMvc.perform(post("/api/game/sessions/{u}/answers", sessionUuid)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"roundId":%d,"selectedIdeophoneId":%d,"responseTimeMs":456}
                                    """.formatted(roundId, selectedId)))
                    .andExpect(status().isOk());
        }

        assertEquals(before, leaderboardTotalElements(token),
                "a completed LADDER session must not add a Meaning Match leaderboard entry");
    }

    @Test
    void startLadderSessionRejectsIncludePractice() throws Exception {
        String token = registerAndGetToken("ladder_practice_" + System.nanoTime());

        mockMvc.perform(post("/api/game/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionName":"CONDITION_1_SOKUON","gameMode":"LADDER","floor":"AUDITORY",
                                 "includePractice":true}
                                """))
                .andExpect(status().isBadRequest());
    }

    private String getFloors(String token) throws Exception {
        return mockMvc.perform(get("/api/game/ladder/floors")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private long leaderboardTotalElements(String token) throws Exception {
        String json = mockMvc.perform(get("/api/leaderboard?page=0&size=50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.totalElements")).longValue();
    }

    private String registerAndGetToken(String username) throws Exception {
        String authJson = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s@example.test","password":"password123"}
                                """.formatted(username, username)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(authJson, "$.token");
    }
}
