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
 * The three-measure research surface: public, read-only, one row per WORD with any data.
 * Like DivergenceHttpTests, this reads shared non-rolled-back data that other tests
 * mutate, so rows are found by known word id and asserted with bounds and invariants --
 * never exact global totals.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TriangulationHttpTests {

    private static final long GOSOGOSO = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void triangulationIsPublicAndRowsAreWellFormed() throws Exception {
        String json = mockMvc.perform(get("/api/research/triangulation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Map<String, Object>> rows = JsonPath.read(json, "$");
        assertTrue(rows.size() > 0, "expected at least one word with data");

        long previousId = 0;
        for (Map<String, Object> row : rows) {
            long ideophoneId = ((Number) row.get("ideophoneId")).longValue();
            assertTrue(ideophoneId > previousId, "rows are ordered by ideophoneId");
            previousId = ideophoneId;

            assertNotNull(row.get("romaji"));
            assertNotNull(row.get("gloss"));

            // Each measure is null exactly when its count is zero -- "no data" never
            // masquerades as "always wrong" or "lowest score".
            assertMeasure(row, "guessCount", "guessAccuracy", 0.0, 1.0);
            assertMeasure(row, "ratingCount", "meanRating", 1.0, 7.0);
            assertMeasure(row, "productionCount", "meanProductionScore", 0.0, 100.0);

            // A row exists only because some measure has data.
            long total = count(row, "guessCount") + count(row, "ratingCount") + count(row, "productionCount");
            assertTrue(total > 0, "row " + ideophoneId + " has no data at all");
        }
    }

    @Test
    void aProductionSurfacesAsProducibilityForThatWord() throws Exception {
        String token = registerAndGetToken("triang_" + System.nanoTime());
        mockMvc.perform(post("/api/productions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ideophoneId":%d,"input":"gosogoso","responseTimeMs":4200}
                                """.formatted(GOSOGOSO)))
                .andExpect(status().isCreated());

        String json = mockMvc.perform(get("/api/research/triangulation"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Map<String, Object>> matches =
                JsonPath.read(json, "$[?(@.ideophoneId == " + GOSOGOSO + ")]");
        assertEquals(1, matches.size(), "one row per word");
        Map<String, Object> row = matches.get(0);

        assertTrue(count(row, "productionCount") >= 1, "the production is counted");
        double mean = ((Number) row.get("meanProductionScore")).doubleValue();
        assertTrue(mean > 0.0 && mean <= 100.0, "meanProductionScore in range: " + mean);
    }

    // Reusing the divergence aggregates verbatim means the shared measures can never
    // disagree between the two endpoints.
    @Test
    void guessAndRatingMeasuresAgreeWithDivergence() throws Exception {
        String triangulation = mockMvc.perform(get("/api/research/triangulation"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String divergence = mockMvc.perform(get("/api/research/divergence"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Map<String, Object>> divergenceRows = JsonPath.read(divergence, "$");
        assertTrue(divergenceRows.size() > 0);

        for (Map<String, Object> divergenceRow : divergenceRows) {
            long ideophoneId = ((Number) divergenceRow.get("ideophoneId")).longValue();
            List<Map<String, Object>> matches =
                    JsonPath.read(triangulation, "$[?(@.ideophoneId == " + ideophoneId + ")]");
            assertEquals(1, matches.size(), "divergence word " + ideophoneId + " is in triangulation");
            Map<String, Object> triangulationRow = matches.get(0);

            assertEquals(divergenceRow.get("guessCount"), triangulationRow.get("guessCount"));
            assertEquals(divergenceRow.get("guessAccuracy"), triangulationRow.get("guessAccuracy"));
            assertEquals(divergenceRow.get("ratingCount"), triangulationRow.get("ratingCount"));
            assertEquals(divergenceRow.get("meanRating"), triangulationRow.get("meanRating"));
        }
    }

    private void assertMeasure(Map<String, Object> row, String countKey, String meanKey,
            double low, double high) {
        long count = count(row, countKey);
        if (count == 0) {
            assertNull(row.get(meanKey), meanKey + " is null when " + countKey + " is 0");
            return;
        }
        double value = ((Number) row.get(meanKey)).doubleValue();
        assertTrue(value >= low && value <= high, meanKey + " in [" + low + "," + high + "]: " + value);
    }

    private long count(Map<String, Object> row, String key) {
        return ((Number) row.get(key)).longValue();
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
        String token = JsonPath.read(authJson, "$.token");
        assertNotNull(token);
        return token;
    }
}
