package io.github.nilsfjp.ideophonearena.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.nilsfjp.ideophonearena.model.Word;
import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import io.github.nilsfjp.ideophonearena.repository.GameSessionRepository;
import io.github.nilsfjp.ideophonearena.repository.WordRepository;
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
 * Population-aggregate divergence endpoint: public, read-only, one row per WORD
 * that has at least one guess or one rating (ADR-0 word grain). The response is
 * shared, non-rolled-back data that other tests mutate, so per-request rows
 * (rated/guessed here) are found by their known word id and asserted with
 * lower bounds and ranges -- never exact global totals -- while the whole-array
 * invariants hold regardless of who else contributed.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DivergenceHttpTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Test
    void divergenceIsPublicAndRowsAreWellFormed() throws Exception {
        String json = mockMvc.perform(get("/api/research/divergence"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertRowInvariants(json);
    }

    @Test
    void ratedWordAppearsWithItsMeanRating() throws Exception {
        // Do not create a word: rate a SEEDED word so the schema's word grain is
        // exercised against real seed data.
        Word word = wordRepository.findById(1L).orElseThrow();
        String suffix = Long.toString(System.nanoTime());
        String token = registerAndGetToken("div_rate_" + suffix);

        mockMvc.perform(post("/api/ratings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ideophoneId":%d,"rating":6}
                                """.formatted(word.getId())))
                .andExpect(status().isCreated());

        Map<String, Object> row = findRow(getDivergence(), word.getId());
        assertNotNull(row, "rated word must appear in divergence");
        // Other users may also have rated this seeded word, so bound rather than
        // assert exact counts/means.
        assertTrue(((Number) row.get("ratingCount")).longValue() >= 1L,
                "ratingCount must include our rating");
        double mean = ((Number) row.get("meanRating")).doubleValue();
        assertTrue(mean >= 1.0 && mean <= 7.0, "meanRating must be in [1,7]");
        assertEquals(word.getGloss(), row.get("gloss"));
    }

    @Test
    void guessedWordAppearsWithItsAccuracy() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String token = registerAndGetToken("div_guess_" + suffix);

        // Start a real, condition-full session so the served round has
        // presentations (TEXT_ONLY sessions have none and would NPE).
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

        String roundJson = mockMvc.perform(get("/api/game/sessions/{uuid}/rounds/next", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long roundId = ((Number) JsonPath.read(roundJson, "$.roundId")).longValue();
        long leftId = ((Number) JsonPath.read(roundJson, "$.left.ideophoneId")).longValue();

        // Answer with the left card. Right or wrong, the answer response names the
        // true target by its word id; that word's row must then carry the guess.
        String answerJson = mockMvc.perform(post("/api/game/sessions/{uuid}/answers", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundId":%d,"selectedIdeophoneId":%d,"responseTimeMs":456}
                                """.formatted(roundId, leftId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long targetId = ((Number) JsonPath.read(answerJson, "$.correctIdeophoneId")).longValue();

        Map<String, Object> row = findRow(getDivergence(), targetId);
        assertNotNull(row, "guessed word must appear in divergence");
        assertTrue(((Number) row.get("guessCount")).longValue() >= 1L,
                "guessCount must include our guess");
        double accuracy = ((Number) row.get("guessAccuracy")).doubleValue();
        assertTrue(accuracy >= 0.0 && accuracy <= 1.0, "guessAccuracy must be in [0,1]");
    }

    private void assertRowInvariants(String json) {
        List<Map<String, Object>> rows = JsonPath.read(json, "$");
        for (Map<String, Object> row : rows) {
            long guessCount = ((Number) row.get("guessCount")).longValue();
            long ratingCount = ((Number) row.get("ratingCount")).longValue();
            assertTrue(guessCount > 0 || ratingCount > 0,
                    "each row must have at least one guess or rating");
            Object guessAccuracy = row.get("guessAccuracy");
            if (guessCount == 0) {
                assertNull(guessAccuracy, "guessAccuracy must be null when there are no guesses");
            } else {
                double accuracy = ((Number) guessAccuracy).doubleValue();
                assertTrue(accuracy >= 0.0 && accuracy <= 1.0, "guessAccuracy must be in [0,1]");
            }
            Object meanRating = row.get("meanRating");
            if (ratingCount == 0) {
                assertNull(meanRating, "meanRating must be null when there are no ratings");
            } else {
                double mean = ((Number) meanRating).doubleValue();
                assertTrue(mean >= 1.0 && mean <= 7.0, "meanRating must be in [1,7]");
            }
        }
    }

    private Map<String, Object> findRow(String json, long ideophoneId) {
        List<Map<String, Object>> rows = JsonPath.read(json, "$");
        return rows.stream()
                .filter(row -> ((Number) row.get("ideophoneId")).longValue() == ideophoneId)
                .findFirst()
                .orElse(null);
    }

    private String getDivergence() throws Exception {
        return mockMvc.perform(get("/api/research/divergence"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
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
