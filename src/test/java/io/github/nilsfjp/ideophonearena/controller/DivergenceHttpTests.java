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
 * Population-aggregate divergence endpoint: public, read-only, one row per
 * ideophone that has at least one guess or one rating. Words created here are
 * fresh, so their rows are deterministic; invariants are also asserted over the
 * whole (shared, non-rolled-back) response.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DivergenceHttpTests {

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
        String suffix = Long.toString(System.nanoTime());
        Ideophone word = ideophoneRepository.save(new Ideophone(
                "テR" + suffix, "テR" + suffix, "てR" + suffix,
                "div-rate-" + suffix, "divergence rating gloss " + suffix,
                "HH", "div-rate-" + suffix + ".m4a", Modality.AUDITORY));
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
        assertEquals(1L, ((Number) row.get("ratingCount")).longValue());
        assertEquals(6.0, ((Number) row.get("meanRating")).doubleValue(), 1e-9);
        // Rated but never guessed: the guess side is absent, encoded as null.
        assertEquals(0L, ((Number) row.get("guessCount")).longValue());
        assertNull(row.get("guessAccuracy"));
        assertEquals("divergence rating gloss " + suffix, row.get("gloss"));
    }

    @Test
    void guessedWordAppearsWithItsAccuracy() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String username = "div_guess_" + suffix;
        String token = registerAndGetToken(username);
        AppUser user = appUserRepository.findByUsername(username).orElseThrow();
        int difficulty = Math.toIntExact(500_000L + (System.nanoTime() % 1_000_000L));

        Ideophone optionA = ideophoneRepository.save(new Ideophone(
                "テGa" + suffix, "テGa" + suffix, "てGa" + suffix,
                "div-guess-a-" + suffix, "divergence guess A " + suffix,
                "HH", "div-guess-a-" + suffix + ".m4a", Modality.AUDITORY));
        Ideophone optionB = ideophoneRepository.save(new Ideophone(
                "テGb" + suffix, "テGb" + suffix, "てGb" + suffix,
                "div-guess-b-" + suffix, "divergence guess B " + suffix,
                "HH", "div-guess-b-" + suffix + ".m4a", Modality.AUDITORY));
        ArenaRound round = arenaRoundRepository.save(new ArenaRound(
                "div guess prompt " + suffix, optionA, optionB, optionA,
                ConditionName.TEXT_ONLY, difficulty, false));

        GameSession session = gameSessionRepository.save(new GameSession(
                user, ConditionName.TEXT_ONLY, difficulty, false, 123456789L));
        String sessionUuid = session.getSessionUuid();

        // The next-round response names the derived target by its gloss, so we
        // can answer it correctly without re-deriving the shuffle here.
        String roundJson = mockMvc.perform(get("/api/game/sessions/{uuid}/rounds/next", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String targetGloss = JsonPath.read(roundJson, "$.targetTranslation");
        Ideophone target = targetGloss.equals(optionA.getGloss()) ? optionA : optionB;

        mockMvc.perform(post("/api/game/sessions/{uuid}/answers", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundId":%d,"selectedIdeophoneId":%d,"responseTimeMs":456}
                                """.formatted(round.getId(), target.getId())))
                .andExpect(status().isOk());

        Map<String, Object> row = findRow(getDivergence(), target.getId());
        assertNotNull(row, "guessed word must appear in divergence");
        assertTrue(((Number) row.get("guessCount")).longValue() >= 1L);
        double accuracy = ((Number) row.get("guessAccuracy")).doubleValue();
        assertTrue(accuracy >= 0.0 && accuracy <= 1.0, "guessAccuracy must be in [0,1]");
        // Guessed but never rated: the rating side is absent, encoded as null.
        assertEquals(0L, ((Number) row.get("ratingCount")).longValue());
        assertNull(row.get("meanRating"));
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
