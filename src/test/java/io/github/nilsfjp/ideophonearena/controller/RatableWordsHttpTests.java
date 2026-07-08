package io.github.nilsfjp.ideophonearena.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.nilsfjp.ideophonearena.model.Trial;
import io.github.nilsfjp.ideophonearena.model.Word;
import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;
import io.github.nilsfjp.ideophonearena.repository.TrialRepository;
import io.github.nilsfjp.ideophonearena.repository.WordRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The Rating Lab pool endpoint, played entirely through the real HTTP flow
 * against the seeded, condition-free trials (M2 word grain, ADR-0). Every
 * session serves the same 47 scored trials; answering a scored round makes both
 * members of its pair ratable, practice words never persist an answer, rated
 * words drop out, and the pool is scoped per user. The M2 grain-heal regression
 * (Test E) proves that answering the same words again under a different script
 * condition does not double-offer them: the pool heals to one row per word.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RatableWordsHttpTests {

    // The seed has 47 scored trials (30 thesis + 17 A/V/I expansion), covering 86
    // distinct words, so a completed session makes exactly that many words ratable.
    // Derived from the live scored trials (expectedScoredWordIds) so it tracks the
    // seed rather than a frozen literal.

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private TrialRepository trialRepository;

    @Test
    void answeredRoundsFeedThePoolPracticeExcludedAndRatingRemovesWords() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String token = registerAndGetToken("ratable_http_" + suffix);

        String sessionUuid = startSession(token, ConditionName.CONDITION_1_SOKUON, true);
        playSessionToCompletion(token, sessionUuid);

        Set<Long> expectedScoredIds = expectedScoredWordIds();
        Set<Long> practiceIds = expectedPracticeWordIds();

        // The whole pool (paged past the size-50 cap): both members of every
        // answered scored pair, each word exactly once, practice never present.
        List<Map<String, Object>> entries = collectAllPoolEntries(token);
        assertEquals(expectedScoredIds.size(), poolTotal(token, "?size=50"),
                "both members of every answered scored round, deduplicated to word grain");
        Set<Long> returnedIds = idsOf(entries);
        assertEquals(expectedScoredIds.size(), returnedIds.size(), "no word appears twice");
        assertEquals(expectedScoredIds, returnedIds);
        for (Long practiceId : practiceIds) {
            assertFalse(returnedIds.contains(practiceId),
                    "practice words must never enter the pool: " + practiceId);
        }

        Map<Long, Word> byId = wordRepository.findAllById(expectedScoredIds).stream()
                .collect(Collectors.toMap(Word::getId, word -> word));
        for (Map<String, Object> entry : entries) {
            Word expected = byId.get(((Number) entry.get("ideophoneId")).longValue());
            assertEquals(expected.getGloss(), entry.get("meaning"),
                    "the meaning must be the word's own gloss, as feedback revealed it");
            assertEquals(expected.getCanonicalForm(), entry.get("canonicalForm"));
            assertEquals(expected.getRomaji(), entry.get("romaji"));
            assertEquals(expected.getStimulusFile(), entry.get("stimulusFile"));
            assertEquals(expected.getModality().name(), entry.get("modality"));
        }

        // The order is deterministic (first-answered, id tiebreak), so a second
        // read returns the identical sequence -- the property multi-device pool
        // parity relies on.
        List<Long> firstOrder = orderedIdsOf(collectAllPoolEntries(token));
        List<Long> secondOrder = orderedIdsOf(collectAllPoolEntries(token));
        assertEquals(firstOrder, secondOrder);

        // Rating one word removes it from the pool and leaves the rest (n -> n-1).
        Long ratedId = firstOrder.get(0);
        rate(token, ratedId, 6);

        assertEquals(expectedScoredIds.size() - 1, poolTotal(token, "?size=50"));
        assertFalse(idsOf(collectAllPoolEntries(token)).contains(ratedId),
                "a rated word must drop out of the pool");
    }

    @Test
    void ratableWordsAreScopedPerUser() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String firstToken = registerAndGetToken("ratable_owner_" + suffix);

        int scoredWords = expectedScoredWordIds().size();
        String sessionUuid = startSession(firstToken, ConditionName.CONDITION_1_SOKUON, false);
        playSessionToCompletion(firstToken, sessionUuid);
        assertEquals(scoredWords, poolTotal(firstToken, "?size=50"));

        // A second user who has answered nothing sees an empty pool -- never the
        // first user's words.
        String secondToken = registerAndGetToken("ratable_other_" + suffix);
        mockMvc.perform(get("/api/game/me/ratable-words")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.entries").isEmpty());

        // The second user rating one of the first user's words must not change
        // the first user's pool.
        Long someWordId = expectedScoredWordIds().iterator().next();
        rate(secondToken, someWordId, 3);
        assertEquals(scoredWords, poolTotal(firstToken, "?size=50"),
                "another user's rating must not shrink this user's pool");
    }

    @Test
    void ratableWordsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/game/me/ratable-words"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ratableWordsPaginationClampsLikeRatings() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String token = registerAndGetToken("ratable_page_" + suffix);

        String sessionUuid = startSession(token, ConditionName.CONDITION_1_SOKUON, false);
        playSessionToCompletion(token, sessionUuid);

        // page/size are honoured; with size=1, totalPages == totalElements == the
        // full scored-word pool.
        int scoredWords = expectedScoredWordIds().size();
        mockMvc.perform(get("/api/game/me/ratable-words?page=0&size=1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(scoredWords))
                .andExpect(jsonPath("$.totalPages").value(scoredWords));

        // A negative page clamps to 0 and an oversized size clamps to the 50 cap,
        // exactly like /me/ratings.
        mockMvc.perform(get("/api/game/me/ratable-words?page=-3&size=999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    void crossConditionReplayHealsToOneRowPerWord() throws Exception {
        // M2 grain-heal (ADR-0): the same 47 scored trials are served in every
        // script condition. A user who answers them under CONDITION_1_SOKUON and
        // then again under CONDITION_2_SOKUON has answered each word twice through
        // two different presentations -- yet, because the pool is keyed by word,
        // every word still appears EXACTLY ONCE. The pre-M2 grain would have
        // double-offered them.
        String suffix = Long.toString(System.nanoTime());
        String token = registerAndGetToken("ratable_heal_" + suffix);

        int scoredWords = expectedScoredWordIds().size();
        String firstSession = startSession(token, ConditionName.CONDITION_1_SOKUON, false);
        playSessionToCompletion(token, firstSession);
        assertEquals(scoredWords, poolTotal(token, "?size=50"));

        String secondSession = startSession(token, ConditionName.CONDITION_2_SOKUON, false);
        playSessionToCompletion(token, secondSession);

        // Still one row per word after the cross-condition replay.
        List<Map<String, Object>> entries = collectAllPoolEntries(token);
        Set<Long> returnedIds = idsOf(entries);
        assertEquals(scoredWords, poolTotal(token, "?size=50"),
                "cross-condition double-offer must be closed by word grain");
        assertEquals(scoredWords, returnedIds.size(), "every word appears exactly once");
        assertEquals(expectedScoredWordIds(), returnedIds);
        assertTrue(returnedIds.size() <= scoredWords, "distinct count never grows past the seeded scored words");
    }

    // --- helpers -----------------------------------------------------------

    private Set<Long> expectedScoredWordIds() {
        Set<Long> ids = new LinkedHashSet<>();
        for (Trial trial : trialRepository.findScoredChoosingTrials()) {
            ids.add(trial.getPairing().getWordA().getId());
            ids.add(trial.getPairing().getWordB().getId());
        }
        return ids;
    }

    private Set<Long> expectedPracticeWordIds() {
        Set<Long> ids = new LinkedHashSet<>();
        for (Trial trial : trialRepository.findByPracticeTrueOrderByIdAsc()) {
            ids.add(trial.getPairing().getWordA().getId());
            ids.add(trial.getPairing().getWordB().getId());
        }
        return ids;
    }

    private Set<Long> idsOf(List<Map<String, Object>> entries) {
        return entries.stream()
                .map(entry -> ((Number) entry.get("ideophoneId")).longValue())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<Long> orderedIdsOf(List<Map<String, Object>> entries) {
        return entries.stream()
                .map(entry -> ((Number) entry.get("ideophoneId")).longValue())
                .toList();
    }

    // Reads the entire pool across pages (the scored-word pool exceeds the size-50
    // cap), preserving server order.
    private List<Map<String, Object>> collectAllPoolEntries(String token) throws Exception {
        List<Map<String, Object>> all = new ArrayList<>();
        int page = 0;
        while (true) {
            String json = getRatableWords(token, "?page=" + page + "&size=50");
            List<Map<String, Object>> entries = JsonPath.read(json, "$.entries");
            all.addAll(entries);
            long total = ((Number) JsonPath.read(json, "$.totalElements")).longValue();
            if (all.size() >= total || entries.isEmpty()) {
                return all;
            }
            page++;
        }
    }

    private int poolTotal(String token, String query) throws Exception {
        return ((Number) JsonPath.read(getRatableWords(token, query), "$.totalElements")).intValue();
    }

    private String getRatableWords(String token, String query) throws Exception {
        return mockMvc.perform(get("/api/game/me/ratable-words" + query)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private void rate(String token, Long wordId, int rating) throws Exception {
        mockMvc.perform(post("/api/ratings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ideophoneId":%d,"rating":%d,"responseTimeMs":1500}
                                """.formatted(wordId, rating)))
                .andExpect(status().isCreated());
    }

    private String startSession(String token, ConditionName condition, boolean includePractice) throws Exception {
        String sessionJson = mockMvc.perform(post("/api/game/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionName":"%s","difficultyLevel":1,"includePractice":%b}
                                """.formatted(condition.name(), includePractice)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(sessionJson, "$.sessionUuid");
    }

    // Answers whatever the session serves (always the left card; correctness is
    // irrelevant to pool membership) until the completion sentinel, so practice
    // ordering is respected and player_answers rows are created through the real
    // flow. 47 scored + up to 2 practice rounds, so 60 iterations is ample.
    private void playSessionToCompletion(String token, String sessionUuid) throws Exception {
        for (int i = 0; i < 60; i++) {
            String roundJson = mockMvc.perform(get("/api/game/sessions/{sessionUuid}/rounds/next", sessionUuid)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            if (Boolean.TRUE.equals(JsonPath.read(roundJson, "$.completed"))) {
                return;
            }
            long roundId = ((Number) JsonPath.read(roundJson, "$.roundId")).longValue();
            long selectedId = ((Number) JsonPath.read(roundJson, "$.left.ideophoneId")).longValue();
            mockMvc.perform(post("/api/game/sessions/{sessionUuid}/answers", sessionUuid)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"roundId":%d,"selectedIdeophoneId":%d,"responseTimeMs":456}
                                    """.formatted(roundId, selectedId)))
                    .andExpect(status().isOk());
        }
        throw new AssertionError("session never reached the completion sentinel");
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
