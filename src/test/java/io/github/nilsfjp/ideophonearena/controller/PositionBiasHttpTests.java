package io.github.nilsfjp.ideophonearena.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * deterministic per-session shuffle (no schema change). The reconstruction is
 * proven end-to-end by playing a real round, picking the left card, and
 * asserting the aggregate's before/after deltas. The population aggregate lives
 * in a shared, non-rolled-back database, so assertions are deltas, not absolutes.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PositionBiasHttpTests {

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
    void positionBiasIsPublicAndWellFormed() throws Exception {
        Map<String, Object> body = snapshot();

        long n = asLong(body, "n");
        assertEquals(n, asLong(body, "leftPickCount") + asLong(body, "rightPickCount"),
                "left + right pick counts must equal n");
        assertEquals(n, asLong(body, "targetTopN") + asLong(body, "targetBottomN"),
                "target-top + target-bottom counts must equal n");
        assertTrue(asLong(body, "targetTopCorrect") <= asLong(body, "targetTopN"), "correct <= n (top)");
        assertTrue(asLong(body, "targetBottomCorrect") <= asLong(body, "targetBottomN"), "correct <= n (bottom)");

        assertRateInRangeOrNull(body, "leftPickRate");
        assertRateInRangeOrNull(body, "targetTopAccuracy");
        assertRateInRangeOrNull(body, "targetBottomAccuracy");

        // d' and criterion are defined together (both null on an empty class).
        Object dPrime = body.get("dPrime");
        Object criterion = body.get("criterion");
        assertEquals(dPrime == null, criterion == null, "d' and criterion are defined together");
        if (dPrime != null) {
            assertTrue(Double.isFinite(((Number) dPrime).doubleValue()), "d' must be finite when present");
            assertTrue(Double.isFinite(((Number) criterion).doubleValue()), "criterion must be finite when present");
        }
    }

    @Test
    void leftPickAndTargetPositionAreReconstructedFromTheSeed() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String username = "posbias_" + suffix;
        String token = registerAndGetToken(username);
        AppUser user = appUserRepository.findByUsername(username).orElseThrow();
        int difficulty = Math.toIntExact(600_000L + (System.nanoTime() % 1_000_000L));

        Ideophone optionA = ideophoneRepository.save(new Ideophone(
                "テPa" + suffix, "テPa" + suffix, "てPa" + suffix,
                "posbias-a-" + suffix, "position bias A " + suffix,
                "HH", "posbias-a-" + suffix + ".m4a", Modality.AUDITORY));
        Ideophone optionB = ideophoneRepository.save(new Ideophone(
                "テPb" + suffix, "テPb" + suffix, "てPb" + suffix,
                "posbias-b-" + suffix, "position bias B " + suffix,
                "HH", "posbias-b-" + suffix + ".m4a", Modality.AUDITORY));
        ArenaRound round = arenaRoundRepository.save(new ArenaRound(
                "position bias prompt " + suffix, optionA, optionB, optionA,
                ConditionName.TEXT_ONLY, difficulty, false));
        GameSession session = gameSessionRepository.save(new GameSession(
                user, ConditionName.TEXT_ONLY, difficulty, false, 987654321L));
        String sessionUuid = session.getSessionUuid();

        Map<String, Object> before = snapshot();

        // The next-round DTO is the seed-derived presentation the player sees:
        // it names the left/right cards and the meaning-line order.
        String roundJson = mockMvc.perform(get("/api/game/sessions/{uuid}/rounds/next", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long leftId = ((Number) JsonPath.read(roundJson, "$.left.ideophoneId")).longValue();
        String targetGloss = JsonPath.read(roundJson, "$.targetTranslation");
        boolean targetMeaningListedFirst = JsonPath.read(roundJson, "$.targetMeaningListedFirst");
        Ideophone target = targetGloss.equals(optionA.getGloss()) ? optionA : optionB;
        boolean correct = leftId == target.getId();  // we always pick the left card

        mockMvc.perform(post("/api/game/sessions/{uuid}/answers", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundId":%d,"selectedIdeophoneId":%d,"responseTimeMs":321}
                                """.formatted(round.getId(), leftId)))
                .andExpect(status().isOk());

        Map<String, Object> after = snapshot();

        assertEquals(asLong(before, "n") + 1, asLong(after, "n"), "one more scored answer");
        assertEquals(asLong(before, "leftPickCount") + 1, asLong(after, "leftPickCount"),
                "picking the left card must increment leftPickCount");
        assertEquals(asLong(before, "rightPickCount"), asLong(after, "rightPickCount"),
                "rightPickCount must be unchanged");

        if (targetMeaningListedFirst) {
            assertEquals(asLong(before, "targetTopN") + 1, asLong(after, "targetTopN"));
            assertEquals(asLong(before, "targetTopCorrect") + (correct ? 1 : 0), asLong(after, "targetTopCorrect"));
            assertEquals(asLong(before, "targetBottomN"), asLong(after, "targetBottomN"));
        } else {
            assertEquals(asLong(before, "targetBottomN") + 1, asLong(after, "targetBottomN"));
            assertEquals(asLong(before, "targetBottomCorrect") + (correct ? 1 : 0),
                    asLong(after, "targetBottomCorrect"));
            assertEquals(asLong(before, "targetTopN"), asLong(after, "targetTopN"));
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
