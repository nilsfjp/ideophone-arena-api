package io.github.nilsfjp.ideophonearena.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.nilsfjp.ideophonearena.model.Ideophone;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
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
class RatingHttpTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdeophoneRepository ideophoneRepository;

    @Test
    void ratingRoundTripCreatesPersistsAndRejectsDuplicatesAndOutOfRange() throws Exception {
        long ideophoneId = anyIdeophoneId();
        String suffix = Long.toString(System.nanoTime());
        String username = "rating_http_" + suffix;
        String token = registerAndGetToken(username);

        // POST a valid rating -> 201 with the response shape (no entity leak).
        String createdJson = mockMvc.perform(post("/api/ratings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ideophoneId":%d,"rating":5,"responseTimeMs":1200}
                                """.formatted(ideophoneId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.ideophoneId").value((int) ideophoneId))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.responseTimeMs").value(1200))
                .andExpect(jsonPath("$.ratedAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number createdId = JsonPath.read(createdJson, "$.id");

        // GET /api/game/me/ratings returns the paginated wrapper containing the new rating.
        String myRatingsJson = mockMvc.perform(get("/api/game/me/ratings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertEquals(1, ((java.util.List<?>) JsonPath.read(myRatingsJson, "$.entries")).size());
        assertEquals(createdId.longValue(), ((Number) JsonPath.read(myRatingsJson, "$.entries[0].id")).longValue());
        assertEquals(ideophoneId, ((Number) JsonPath.read(myRatingsJson, "$.entries[0].ideophoneId")).longValue());
        assertEquals(5, ((Number) JsonPath.read(myRatingsJson, "$.entries[0].rating")).intValue());

        // Re-rating the same word for the same user -> 409.
        mockMvc.perform(post("/api/ratings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ideophoneId":%d,"rating":3}
                                """.formatted(ideophoneId)))
                .andExpect(status().isConflict());

        // rating above the 1..7 range -> 400 with a validationErrors map.
        mockMvc.perform(post("/api/ratings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ideophoneId":%d,"rating":8}
                                """.formatted(ideophoneId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.rating").exists());

        // rating below the 1..7 range -> 400.
        mockMvc.perform(post("/api/ratings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ideophoneId":%d,"rating":0}
                                """.formatted(ideophoneId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.rating").exists());
    }

    @Test
    void myRatingsArePaginatedAndSizeIsClamped() throws Exception {
        java.util.List<Long> ideophoneIds = distinctIdeophoneIds(3);
        String suffix = Long.toString(System.nanoTime());
        String token = registerAndGetToken("rating_page_" + suffix);

        int[] ratingValues = {5, 4, 6};
        for (int i = 0; i < ideophoneIds.size(); i++) {
            mockMvc.perform(post("/api/ratings")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"ideophoneId":%d,"rating":%d}
                                    """.formatted(ideophoneIds.get(i), ratingValues[i])))
                    .andExpect(status().isCreated());
        }

        // First page of size 2: two of three entries, wrapper metadata present.
        mockMvc.perform(get("/api/game/me/ratings?page=0&size=2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        // Second page holds the remaining entry.
        mockMvc.perform(get("/api/game/me/ratings?page=1&size=2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.page").value(1));

        // size above the cap is clamped to 50; all three entries fit on one page.
        mockMvc.perform(get("/api/game/me/ratings?size=999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.entries.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void createRatingRequiresAuthentication() throws Exception {
        long ideophoneId = anyIdeophoneId();
        mockMvc.perform(post("/api/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ideophoneId":%d,"rating":4}
                                """.formatted(ideophoneId)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createRatingRejectsUnknownIdeophone() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String token = registerAndGetToken("rating_unknown_" + suffix);
        mockMvc.perform(post("/api/ratings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ideophoneId":999999999,"rating":4}
                                """))
                .andExpect(status().isNotFound());
    }

    private long anyIdeophoneId() {
        return ideophoneRepository.findAll().stream()
                .findFirst()
                .map(Ideophone::getId)
                .orElseGet(() -> ideophoneRepository.save(new Ideophone(
                        "テスト" + System.nanoTime(),
                        "テスト",
                        "テスト",
                        "rating-test-" + System.nanoTime(),
                        "rating test gloss",
                        "RT" + System.nanoTime(),
                        "rating-test-" + System.nanoTime() + ".mp4",
                        Modality.AUDITORY
                )).getId());
    }

    private java.util.List<Long> distinctIdeophoneIds(int n) {
        return ideophoneRepository.findAll().stream()
                .map(Ideophone::getId)
                .distinct()
                .limit(n)
                .toList();
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
        assertTrue(token.length() > 0);
        return token;
    }
}
