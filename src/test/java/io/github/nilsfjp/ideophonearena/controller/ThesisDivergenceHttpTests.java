package io.github.nilsfjp.ideophonearena.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The Observatory thesis layer (NIL-54): GET /api/research/thesis/divergence is
 * the inverted-Rider-A counterpart of /api/research/divergence, computed over
 * the seeded thesis_p% cohort only. Because each of the 30 target words was
 * answered and rated by all 36 thesis participants, the endpoint is deterministic
 * regardless of what live-player rows other tests add (they are never thesis_p%),
 * and its per-modality rollup reproduces the vendored thesis figures.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ThesisDivergenceHttpTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void thesisDivergenceIsPublicAndReconstructsTheVendoredPerModalityAccuracy() throws Exception {
        String json = mockMvc.perform(get("/api/research/thesis/divergence"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Map<String, Object>> rows = JsonPath.read(json, "$");
        assertEquals(30, rows.size(), "one row per thesis target word (targets and foils are disjoint)");

        Map<String, long[]> perModality = new HashMap<>();   // modality -> [guessTotal, correctTotal]
        for (Map<String, Object> row : rows) {
            long guessCount = ((Number) row.get("guessCount")).longValue();
            long ratingCount = ((Number) row.get("ratingCount")).longValue();
            assertEquals(36L, guessCount, "each target word was answered by all 36 participants");
            assertEquals(36L, ratingCount, "each target word was rated by all 36 participants");

            double accuracy = ((Number) row.get("guessAccuracy")).doubleValue();
            assertTrue(accuracy >= 0.0 && accuracy <= 1.0, "guessAccuracy must be in [0,1]");
            double meanRating = ((Number) row.get("meanRating")).doubleValue();
            assertTrue(meanRating >= 1.0 && meanRating <= 7.0, "meanRating must be in [1,7]");

            long correct = Math.round(accuracy * guessCount);   // accuracy = correct/36, so this is exact
            long[] counts = perModality.computeIfAbsent((String) row.get("modality"), key -> new long[2]);
            counts[0] += guessCount;
            counts[1] += correct;
        }

        // The thesis vendored figures: 68.6 / 64.2 / 59.7 percent per modality.
        assertArrayEquals(new long[] {360, 247}, perModality.get("AUDITORY"), "auditory 247/360 = 68.6%");
        assertArrayEquals(new long[] {360, 231}, perModality.get("VISUAL"), "visual 231/360 = 64.2%");
        assertArrayEquals(new long[] {360, 215}, perModality.get("INTEROCEPTIVE"), "interoceptive 215/360 = 59.7%");
    }
}
