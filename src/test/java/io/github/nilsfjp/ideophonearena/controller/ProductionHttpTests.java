package io.github.nilsfjp.ideophonearena.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The production vertical end to end. Each test mints a nanoTime-unique user, so the
 * (user, word) uniqueness and the per-user prompt cycle are isolated without rollback
 * against the shared live database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductionHttpTests {

    // The first four seeded prompts, one per modality in cycle order.
    private static final long FIRST_AUDITORY = 1L;
    private static final long FIRST_VISUAL = 21L;
    private static final long FIRST_HAPTIC = 79L;
    private static final long FIRST_INTEROCEPTIVE = 41L;

    // The word the adjudicated Word Mint mockup scores pikapika against.
    private static final long DOKIDOKI = 60L;

    // The seeded producible universe: words in >= 1 non-practice trial, across the four
    // cycle modalities. This is the {n} of Word Mint's "word {i} of {n}".
    private static final int PRODUCIBLE_TOTAL = 94;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void aFreshCallerIsPromptedWithTheLowestAuditoryWordAndNoFormLeaks() throws Exception {
        String token = registerAndGetToken("prod_next_" + System.nanoTime());

        mockMvc.perform(get("/api/productions/next").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.ideophoneId").value((int) FIRST_AUDITORY))
                .andExpect(jsonPath("$.modality").value("AUDITORY"))
                .andExpect(jsonPath("$.gloss").isNotEmpty())
                .andExpect(jsonPath("$.totalProducible").value(PRODUCIBLE_TOTAL))
                // The meaning is the whole prompt: no romaji, no kana, no audio pre-submit.
                .andExpect(jsonPath("$.romaji").doesNotExist())
                .andExpect(jsonPath("$.displayForm").doesNotExist())
                .andExpect(jsonPath("$.stimulusUrl").doesNotExist());
    }

    // {n} is the universe, not the remainder: minting a word advances {i}, never shrinks {n}.
    @Test
    void theProducibleTotalIsCallerInvariantAcrossAMint() throws Exception {
        String token = registerAndGetToken("prod_total_" + System.nanoTime());

        produce(token, FIRST_AUDITORY, "gorogoro").andExpect(status().isCreated());

        mockMvc.perform(get("/api/productions/next").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducible").value(PRODUCIBLE_TOTAL));
    }

    @Test
    void thePromptCycleWalksAuditoryVisualHapticInteroceptive() throws Exception {
        String token = registerAndGetToken("prod_cycle_" + System.nanoTime());

        assertEquals(FIRST_AUDITORY, nextIdeophoneId(token));
        produce(token, FIRST_AUDITORY, "gosogoso").andExpect(status().isCreated());

        assertEquals(FIRST_VISUAL, nextIdeophoneId(token));
        produce(token, FIRST_VISUAL, "pikapika").andExpect(status().isCreated());

        assertEquals(FIRST_HAPTIC, nextIdeophoneId(token));
        produce(token, FIRST_HAPTIC, "sarasara").andExpect(status().isCreated());

        assertEquals(FIRST_INTEROCEPTIVE, nextIdeophoneId(token));
        produce(token, FIRST_INTEROCEPTIVE, "dokidoki").andExpect(status().isCreated());

        // ...and back to AUDITORY, on the next unproduced word.
        assertEquals(2L, nextIdeophoneId(token));
    }

    @Test
    void anExactFormScoresOneHundredAndRevealsTheRealWord() throws Exception {
        String token = registerAndGetToken("prod_exact_" + System.nanoTime());

        produce(token, FIRST_AUDITORY, "gosogoso")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.ideophoneId").value((int) FIRST_AUDITORY))
                .andExpect(jsonPath("$.input").value("gosogoso"))
                .andExpect(jsonPath("$.similarityScore").value(100))
                .andExpect(jsonPath("$.features.length()").value(7))
                .andExpect(jsonPath("$.features[0].feature").value("redup"))
                .andExpect(jsonPath("$.features[0].yours").value(true))
                .andExpect(jsonPath("$.features[0].matched").value(true))
                .andExpect(jsonPath("$.features[6].feature").value("moraCount"))
                .andExpect(jsonPath("$.features[6].yours").value(4))
                .andExpect(jsonPath("$.target.displayForm").isNotEmpty())
                .andExpect(jsonPath("$.target.romaji").value("gosogoso"))
                .andExpect(jsonPath("$.target.gloss").isNotEmpty())
                .andExpect(jsonPath("$.target.stimulusUrl").value("/stimuli/audio/a0h-gosogoso.m4a"));
    }

    // The adjudicated worked example, over HTTP: pikapika against dokidoki scores 78 and
    // the mismatched chips are the voiced onset and the vowel weight.
    @Test
    void theWorkedExampleScoresSeventyEightWithLegibleChips() throws Exception {
        String token = registerAndGetToken("prod_worked_" + System.nanoTime());

        produce(token, DOKIDOKI, "pikapika")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.similarityScore").value(78))
                .andExpect(jsonPath("$.features[4].feature").value("voicedOnset"))
                .andExpect(jsonPath("$.features[4].yours").value(false))
                .andExpect(jsonPath("$.features[4].target").value(true))
                .andExpect(jsonPath("$.features[4].matched").value(false))
                .andExpect(jsonPath("$.features[5].feature").value("heavyVowelRatio"))
                .andExpect(jsonPath("$.features[5].matched").value(false))
                .andExpect(jsonPath("$.features[6].matched").value(true));
    }

    @Test
    void oneProductionPerWordPerUser() throws Exception {
        String token = registerAndGetToken("prod_dupe_" + System.nanoTime());

        produce(token, FIRST_AUDITORY, "gosogoso").andExpect(status().isCreated());
        produce(token, FIRST_AUDITORY, "kasakasa").andExpect(status().isConflict());
    }

    // The one-shot promise: a typo must not burn the attempt.
    @Test
    void unparseableInputIsRejectedWithoutConsumingTheAttempt() throws Exception {
        String token = registerAndGetToken("prod_parse_" + System.nanoTime());

        produce(token, FIRST_AUDITORY, "ngrk")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.input").isNotEmpty());

        // The word is still on offer, and still produceable.
        assertEquals(FIRST_AUDITORY, nextIdeophoneId(token));
        produce(token, FIRST_AUDITORY, "gosogoso").andExpect(status().isCreated());
    }

    @Test
    void inputIsGatedTrimmedAndLowercased() throws Exception {
        String token = registerAndGetToken("prod_gate_" + System.nanoTime());

        produce(token, FIRST_AUDITORY, "  GosoGoso  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.input").value("gosogoso"))
                .andExpect(jsonPath("$.similarityScore").value(100));

        produce(token, FIRST_VISUAL, "pika pika")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.input").isNotEmpty());
        produce(token, FIRST_VISUAL, "a")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.input").isNotEmpty());
        produce(token, FIRST_VISUAL, "pika3")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.input").isNotEmpty());
    }

    @Test
    void responseTimeOutOfRangeIsRejected() throws Exception {
        String token = registerAndGetToken("prod_rt_" + System.nanoTime());

        mockMvc.perform(post("/api/productions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ideophoneId":%d,"input":"gorogoro","responseTimeMs":600001}
                                """.formatted(FIRST_AUDITORY)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.responseTimeMs").exists());
    }

    @Test
    void unknownIdeophoneIsNotFound() throws Exception {
        String token = registerAndGetToken("prod_404_" + System.nanoTime());
        produce(token, 999999999L, "gorogoro").andExpect(status().isNotFound());
    }

    @Test
    void unknownSessionIsNotFoundAndAnotherUsersSessionIsForbidden() throws Exception {
        String ownerToken = registerAndGetToken("prod_owner_" + System.nanoTime());
        String otherToken = registerAndGetToken("prod_other_" + System.nanoTime());

        mockMvc.perform(post("/api/productions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ideophoneId":%d,"input":"gorogoro","sessionUuid":"not-a-real-session"}
                                """.formatted(FIRST_AUDITORY)))
                .andExpect(status().isNotFound());

        String sessionUuid = startSession(ownerToken);
        mockMvc.perform(post("/api/productions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ideophoneId":%d,"input":"gorogoro","sessionUuid":"%s"}
                                """.formatted(FIRST_AUDITORY, sessionUuid)))
                .andExpect(status().isForbidden());

        // The owner's own session is valid provenance.
        mockMvc.perform(post("/api/productions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ideophoneId":%d,"input":"gorogoro","sessionUuid":"%s"}
                                """.formatted(FIRST_AUDITORY, sessionUuid)))
                .andExpect(status().isCreated());
    }

    @Test
    void myProductionsIsAPaginatedWrapperMostRecentFirst() throws Exception {
        String token = registerAndGetToken("prod_mine_" + System.nanoTime());
        produce(token, FIRST_AUDITORY, "gosogoso").andExpect(status().isCreated());
        produce(token, FIRST_VISUAL, "kukkiri").andExpect(status().isCreated());

        mockMvc.perform(get("/api/game/me/productions").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries").isArray())
                .andExpect(jsonPath("$.entries.length()").value(2))
                .andExpect(jsonPath("$.entries[0].ideophoneId").value((int) FIRST_VISUAL))
                .andExpect(jsonPath("$.entries[0].input").value("kukkiri"))
                .andExpect(jsonPath("$.entries[0].similarityScore").exists())
                .andExpect(jsonPath("$.entries[0].createdAt").exists())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        // Out-of-range paging clamps rather than rejects (page >= 0, size 1..50).
        mockMvc.perform(get("/api/game/me/productions?page=-5&size=999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    void allProductionEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/productions/next")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/game/me/productions")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/productions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ideophoneId":1,"input":"gorogoro"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions produce(String token, long ideophoneId,
            String input) throws Exception {
        return mockMvc.perform(post("/api/productions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"ideophoneId":%d,"input":"%s","responseTimeMs":5200}
                        """.formatted(ideophoneId, input)));
    }

    private long nextIdeophoneId(String token) throws Exception {
        String json = mockMvc.perform(get("/api/productions/next")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number id = JsonPath.read(json, "$.ideophoneId");
        return id.longValue();
    }

    private String startSession(String token) throws Exception {
        String json = mockMvc.perform(post("/api/game/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionName":"CONDITION_1_SOKUON"}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(json, "$.sessionUuid");
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
