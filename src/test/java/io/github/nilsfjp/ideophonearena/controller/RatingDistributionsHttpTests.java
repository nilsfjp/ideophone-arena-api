package io.github.nilsfjp.ideophonearena.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.nilsfjp.ideophonearena.model.Ideophone;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import io.github.nilsfjp.ideophonearena.repository.IdeophoneRepository;
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
 * Per-modality distribution of the 1-7 iconicity ratings: public, read-only,
 * per-value counts. A modality that has any ratings gets a dense 1-7 grid;
 * `byModalityN` carries each present modality's total. Assertions use
 * before/after deltas because the aggregate is population-wide over a shared,
 * non-rolled-back database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RatingDistributionsHttpTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdeophoneRepository ideophoneRepository;

    @Test
    void distributionsArePublicAndWellFormed() throws Exception {
        String json = mockMvc.perform(get("/api/research/rating-distributions"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Map<String, Object>> cells = JsonPath.read(json, "$.distributions");
        Map<String, Object> byModalityN = JsonPath.read(json, "$.byModalityN");

        for (Map<String, Object> cell : cells) {
            long count = ((Number) cell.get("count")).longValue();
            int value = ((Number) cell.get("ratingValue")).intValue();
            assertTrue(count >= 0, "counts are non-negative");
            assertTrue(value >= 1 && value <= 7, "rating values are 1..7");
        }

        // Every modality that appears must have a full, dense 1-7 grid whose
        // counts sum to its declared n.
        for (String modality : byModalityN.keySet()) {
            long declaredN = ((Number) byModalityN.get(modality)).longValue();
            long cellsForModality = cells.stream()
                    .filter(cell -> modality.equals(cell.get("modality")))
                    .count();
            long summedCount = cells.stream()
                    .filter(cell -> modality.equals(cell.get("modality")))
                    .mapToLong(cell -> ((Number) cell.get("count")).longValue())
                    .sum();
            assertEquals(7L, cellsForModality, "present modality " + modality + " must have a dense 1-7 grid");
            assertEquals(declaredN, summedCount, "per-value bins must sum to byModalityN[" + modality + "]");
        }
    }

    @Test
    void ratingLandsInItsModalityValueCellAndBumpsModalityN() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String before = getDistributions();
        long cellBefore = cellCount(before, "AUDITORY", 5);
        long nBefore = modalityN(before, "AUDITORY");

        Ideophone word = ideophoneRepository.save(new Ideophone(
                "テD" + suffix, "テD" + suffix, "てD" + suffix,
                "rdist-" + suffix, "rating distribution gloss " + suffix,
                "HH", "rdist-" + suffix + ".m4a", Modality.AUDITORY));
        String token = registerAndGetToken("rdist_" + suffix);

        mockMvc.perform(post("/api/ratings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ideophoneId":%d,"rating":5}
                                """.formatted(word.getId())))
                .andExpect(status().isCreated());

        String after = getDistributions();
        assertEquals(cellBefore + 1, cellCount(after, "AUDITORY", 5),
                "the new rating must land in the (AUDITORY, 5) cell");
        assertEquals(nBefore + 1, modalityN(after, "AUDITORY"),
                "byModalityN.AUDITORY must grow by one");
        assertEquals(7L, cellsForModality(after, "AUDITORY"),
                "AUDITORY now has ratings, so its grid must be dense 1-7");
    }

    private long cellCount(String json, String modality, int ratingValue) {
        List<Map<String, Object>> cells = JsonPath.read(json, "$.distributions");
        return cells.stream()
                .filter(cell -> modality.equals(cell.get("modality"))
                        && ((Number) cell.get("ratingValue")).intValue() == ratingValue)
                .mapToLong(cell -> ((Number) cell.get("count")).longValue())
                .findFirst()
                .orElse(0L);
    }

    private long modalityN(String json, String modality) {
        Map<String, Object> byModalityN = JsonPath.read(json, "$.byModalityN");
        Object value = byModalityN.get(modality);
        return value == null ? 0L : ((Number) value).longValue();
    }

    private long cellsForModality(String json, String modality) {
        List<Map<String, Object>> cells = JsonPath.read(json, "$.distributions");
        return cells.stream().filter(cell -> modality.equals(cell.get("modality"))).count();
    }

    private String getDistributions() throws Exception {
        return mockMvc.perform(get("/api/research/rating-distributions"))
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
