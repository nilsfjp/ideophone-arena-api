package io.github.nilsfjp.ideophonearena.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * The Rating Lab pool endpoint against an isolated fixture (unique difficulty,
 * sessions created through the repository): answered scored rounds contribute
 * both members exactly once, practice words never appear (their answers are
 * never persisted), rated words drop out, and the pool is scoped per user.
 * Rounds are played through the real HTTP flow so the pool reflects genuine
 * player_answers rows.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RatableWordsHttpTests {

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
    void answeredRoundsFeedThePoolPracticeExcludedAndRatingRemovesWords() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String username = "ratable_http_" + suffix;
        String token = registerAndGetToken(username);
        AppUser user = appUserRepository.findByUsername(username).orElseThrow();
        int difficulty = Math.toIntExact(600_000L + (System.nanoTime() % 1_000_000L));

        ArenaRound practiceOne = fixtureRound("rp0", suffix, difficulty, true);
        ArenaRound practiceTwo = fixtureRound("rp1", suffix, difficulty, true);
        ArenaRound scoredOne = fixtureRound("rs0", suffix, difficulty, false);
        ArenaRound scoredTwo = fixtureRound("rs1", suffix, difficulty, false);
        // A third scored round reusing the first round's pair: encountering a
        // word twice must not duplicate it in the pool.
        arenaRoundRepository.save(new ArenaRound(
                scoredOne.getPrompt(),
                scoredOne.getLeftIdeophone(),
                scoredOne.getRightIdeophone(),
                scoredOne.getLeftIdeophone(),
                ConditionName.TEXT_ONLY,
                difficulty,
                false
        ));

        GameSession session = gameSessionRepository.save(new GameSession(
                user, ConditionName.TEXT_ONLY, difficulty, true));
        playSessionToCompletion(token, session.getSessionUuid());

        Set<Long> scoredIds = Set.of(
                scoredOne.getLeftIdeophone().getId(), scoredOne.getRightIdeophone().getId(),
                scoredTwo.getLeftIdeophone().getId(), scoredTwo.getRightIdeophone().getId());
        Set<Long> practiceIds = Set.of(
                practiceOne.getLeftIdeophone().getId(), practiceOne.getRightIdeophone().getId(),
                practiceTwo.getLeftIdeophone().getId(), practiceTwo.getRightIdeophone().getId());

        String poolJson = getRatableWords(token, "?size=50");
        assertEquals(4, ((Number) JsonPath.read(poolJson, "$.totalElements")).intValue(),
                "both members of each answered scored round, deduplicated: " + poolJson);
        List<Map<String, Object>> entries = JsonPath.read(poolJson, "$.entries");
        Set<Long> returnedIds = entries.stream()
                .map(entry -> ((Number) entry.get("ideophoneId")).longValue())
                .collect(Collectors.toSet());
        assertEquals(scoredIds, returnedIds);
        for (Long practiceId : practiceIds) {
            assertFalse(returnedIds.contains(practiceId),
                    "practice words must never enter the pool: " + practiceId);
        }

        Map<Long, Ideophone> byId = ideophoneRepository.findAllById(scoredIds).stream()
                .collect(Collectors.toMap(Ideophone::getId, ideophone -> ideophone));
        for (Map<String, Object> entry : entries) {
            Ideophone expected = byId.get(((Number) entry.get("ideophoneId")).longValue());
            assertEquals(expected.getGloss(), entry.get("meaning"),
                    "the meaning must be the word's own gloss, as feedback revealed it");
            assertEquals(expected.getCanonicalForm(), entry.get("canonicalForm"));
            assertEquals(expected.getRomaji(), entry.get("romaji"));
            assertEquals(expected.getStimulusFile(), entry.get("stimulusFile"));
            assertEquals(expected.getModality().name(), entry.get("modality"));
        }

        // The order is deterministic (first encounter, id tiebreak), so a
        // second read returns the identical sequence -- the property the
        // multi-device pool parity relies on.
        List<Long> firstOrder = entryIds(poolJson);
        assertEquals(firstOrder, entryIds(getRatableWords(token, "?size=50")));

        // Rating one word removes it from the pool and leaves the rest.
        Long ratedId = scoredOne.getLeftIdeophone().getId();
        mockMvc.perform(post("/api/ratings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ideophoneId":%d,"rating":6,"responseTimeMs":1500}
                                """.formatted(ratedId)))
                .andExpect(status().isCreated());

        String afterRatingJson = getRatableWords(token, "?size=50");
        assertEquals(3, ((Number) JsonPath.read(afterRatingJson, "$.totalElements")).intValue());
        assertFalse(entryIds(afterRatingJson).contains(ratedId),
                "a rated word must drop out of the pool");
    }

    @Test
    void ratableWordsAreScopedPerUser() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String firstUsername = "ratable_owner_" + suffix;
        String firstToken = registerAndGetToken(firstUsername);
        AppUser firstUser = appUserRepository.findByUsername(firstUsername).orElseThrow();
        int difficulty = Math.toIntExact(700_000L + (System.nanoTime() % 1_000_000L));
        ArenaRound scoredRound = fixtureRound("iso", suffix, difficulty, false);

        GameSession session = gameSessionRepository.save(new GameSession(
                firstUser, ConditionName.TEXT_ONLY, difficulty));
        playSessionToCompletion(firstToken, session.getSessionUuid());

        String ownerJson = getRatableWords(firstToken, "");
        assertEquals(2, ((Number) JsonPath.read(ownerJson, "$.totalElements")).intValue());

        // A second user who has answered nothing sees an empty pool -- never
        // the first user's words.
        String secondToken = registerAndGetToken("ratable_other_" + suffix);
        mockMvc.perform(get("/api/game/me/ratable-words")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.entries").isEmpty());

        // The second user rating one of the first user's words must not
        // change the first user's pool.
        mockMvc.perform(post("/api/ratings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ideophoneId":%d,"rating":3,"responseTimeMs":1200}
                                """.formatted(scoredRound.getLeftIdeophone().getId())))
                .andExpect(status().isCreated());
        assertEquals(2, ((Number) JsonPath.read(getRatableWords(firstToken, ""), "$.totalElements")).intValue(),
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
        String username = "ratable_page_" + suffix;
        String token = registerAndGetToken(username);
        AppUser user = appUserRepository.findByUsername(username).orElseThrow();
        int difficulty = Math.toIntExact(800_000L + (System.nanoTime() % 1_000_000L));
        fixtureRound("pg", suffix, difficulty, false);

        GameSession session = gameSessionRepository.save(new GameSession(
                user, ConditionName.TEXT_ONLY, difficulty));
        playSessionToCompletion(token, session.getSessionUuid());

        mockMvc.perform(get("/api/game/me/ratable-words?page=0&size=1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/game/me/ratable-words?page=-3&size=999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50));
    }

    private List<Long> entryIds(String poolJson) {
        List<Number> raw = JsonPath.read(poolJson, "$.entries[*].ideophoneId");
        return raw.stream().map(Number::longValue).toList();
    }

    private String getRatableWords(String token, String query) throws Exception {
        return mockMvc.perform(get("/api/game/me/ratable-words" + query)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    // Answers whatever the session serves (always the left card; correctness
    // is irrelevant to pool membership) until the completion sentinel, so
    // practice ordering rules are respected and player_answers rows are
    // created through the real flow.
    private void playSessionToCompletion(String token, String sessionUuid) throws Exception {
        for (int i = 0; i < 20; i++) {
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

    private ArenaRound fixtureRound(String tag, String suffix, int difficulty, boolean practice) {
        String prompt = tag + " target " + suffix;
        Ideophone thesisTarget = ideophone(tag + "l", suffix, prompt);
        Ideophone distractor = ideophone(tag + "r", suffix, tag + " distractor " + suffix);
        return arenaRoundRepository.save(new ArenaRound(
                prompt,
                thesisTarget,
                distractor,
                thesisTarget,
                ConditionName.TEXT_ONLY,
                difficulty,
                practice
        ));
    }

    private Ideophone ideophone(String tag, String suffix, String gloss) {
        String kana = tag + suffix.substring(suffix.length() - 6);
        return ideophoneRepository.save(new Ideophone(
                kana,
                kana,
                kana,
                tag + "-" + suffix,
                gloss,
                tag.toUpperCase() + suffix.substring(suffix.length() - 8),
                tag + "-" + suffix + ".m4a",
                Modality.AUDITORY
        ));
    }
}
