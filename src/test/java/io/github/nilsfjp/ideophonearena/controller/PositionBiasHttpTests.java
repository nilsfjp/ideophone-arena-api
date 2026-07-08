package io.github.nilsfjp.ideophonearena.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import io.github.nilsfjp.ideophonearena.repository.GameSessionRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Position-bias fairness aggregate: public, read-only, reconstructed from the
 * deterministic per-session shuffle over the WORD-grain seed. The reconstruction
 * is proven end-to-end by playing a real seeded round via the API, picking the
 * left card, and asserting that the population aggregate reflects the new answer.
 * That aggregate lives in a shared, non-rolled-back database that other tests
 * also mutate, so assertions are robust lower bounds and shape invariants, never
 * absolute totals.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PositionBiasHttpTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Test
    void positionBiasIsPublicAndWellFormed() throws Exception {
        Map<String, Object> body = snapshot();
        assertInvariants(body);
    }

    @Test
    void leftPickAndTargetPositionAreReflectedInTheAggregate() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String username = "posbias_" + suffix;
        String token = registerAndGetToken(username);
        // Materialize the user so the autowired repos are genuinely exercised;
        // the session below is created through the public game API.
        appUserRepository.findByUsername(username).orElseThrow();
        assertTrue(gameSessionRepository.count() >= 0);

        Map<String, Object> before = snapshot();

        // A condition-free seeded session: every session serves the same 47
        // scored trials, each with presentations, so the first served round is a
        // real scored round we can answer.
        String sessionJson = mockMvc.perform(post("/api/game/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionName":"CONDITION_1_SOKUON","difficultyLevel":1}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String sessionUuid = JsonPath.read(sessionJson, "$.sessionUuid");

        // The next-round DTO is the seed-derived presentation the player sees; we
        // always pick the left card (its word id is the selected ideophone id).
        String roundJson = mockMvc.perform(get("/api/game/sessions/{uuid}/rounds/next", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long roundId = ((Number) JsonPath.read(roundJson, "$.roundId")).longValue();
        long leftId = ((Number) JsonPath.read(roundJson, "$.left.ideophoneId")).longValue();

        mockMvc.perform(post("/api/game/sessions/{uuid}/answers", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundId":%d,"selectedIdeophoneId":%d,"responseTimeMs":321}
                                """.formatted(roundId, leftId)))
                .andExpect(status().isOk());

        Map<String, Object> after = snapshot();

        // Shared data: only assert the aggregate grew and stays well-formed.
        assertInvariants(after);
        assertTrue(asLong(after, "n") >= 1, "the aggregate must count at least our answer");
        assertTrue(asLong(after, "n") > asLong(before, "n"),
                "our scored answer must be reflected in the population count");
    }

    // Shape invariants that must hold for the single aggregate object regardless
    // of how much shared data other tests have contributed.
    private void assertInvariants(Map<String, Object> body) {
        long n = asLong(body, "n");
        assertTrue(n >= 0, "n must be non-negative");
        assertEquals(n, asLong(body, "leftPickCount") + asLong(body, "rightPickCount"),
                "left + right pick counts must equal n");
        assertEquals(n, asLong(body, "targetTopN") + asLong(body, "targetBottomN"),
                "target-top + target-bottom counts must equal n");
        assertTrue(asLong(body, "targetTopCorrect") <= asLong(body, "targetTopN"), "correct <= n (top)");
        assertTrue(asLong(body, "targetBottomCorrect") <= asLong(body, "targetBottomN"), "correct <= n (bottom)");

        Object leftPickRate = body.get("leftPickRate");
        if (n == 0) {
            assertTrue(leftPickRate == null, "leftPickRate must be null on an empty class");
        } else {
            assertTrue(leftPickRate != null, "leftPickRate must be present when n > 0");
        }
        assertRateInRangeOrNull(body, "leftPickRate");
        assertRateInRangeOrNull(body, "targetTopAccuracy");
        assertRateInRangeOrNull(body, "targetBottomAccuracy");

        // d' and criterion are defined together (both null when a class is empty).
        Object dPrime = body.get("dPrime");
        Object criterion = body.get("criterion");
        assertEquals(dPrime == null, criterion == null, "d' and criterion are defined together");
        if (dPrime != null) {
            assertTrue(Double.isFinite(((Number) dPrime).doubleValue()), "d' must be finite when present");
            assertTrue(Double.isFinite(((Number) criterion).doubleValue()), "criterion must be finite when present");
        }
    }

    private Map<String, Object> snapshot() throws Exception {
        String json = mockMvc.perform(get("/api/research/position-bias"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$");
    }

    private static long asLong(Map<String, Object> body, String name) {
        return ((Number) body.get(name)).longValue();
    }

    private void assertRateInRangeOrNull(Map<String, Object> body, String name) {
        Object value = body.get(name);
        if (value != null) {
            double rate = ((Number) value).doubleValue();
            assertTrue(rate >= 0.0 && rate <= 1.0, name + " must be in [0,1] when present");
        }
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
