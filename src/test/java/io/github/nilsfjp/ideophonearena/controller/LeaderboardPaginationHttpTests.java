package io.github.nilsfjp.ideophonearena.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
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
import io.github.nilsfjp.ideophonearena.model.PlayerAnswer;
import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import io.github.nilsfjp.ideophonearena.repository.ArenaRoundRepository;
import io.github.nilsfjp.ideophonearena.repository.GameSessionRepository;
import io.github.nilsfjp.ideophonearena.repository.IdeophoneRepository;
import io.github.nilsfjp.ideophonearena.repository.PlayerAnswerRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LeaderboardPaginationHttpTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private IdeophoneRepository ideophoneRepository;

    @Autowired
    private ArenaRoundRepository arenaRoundRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private PlayerAnswerRepository playerAnswerRepository;

    @Test
    void leaderboardReturnsPaginationWrapperWithDefaults() throws Exception {
        mockMvc.perform(get("/api/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries").isArray())
                .andExpect(jsonPath("$.entries.length()").value(lessThanOrEqualTo(10)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.totalPages").value(greaterThanOrEqualTo(0)));
    }

    @Test
    void leaderboardCapsSizeAtFifty() throws Exception {
        mockMvc.perform(get("/api/leaderboard").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    void leaderboardHonorsExplicitPageAndSize() throws Exception {
        mockMvc.perform(get("/api/leaderboard").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.entries.length()").value(lessThanOrEqualTo(5)));
    }

    @Test
    void leaderboardClampsNegativeOrZeroParams() throws Exception {
        mockMvc.perform(get("/api/leaderboard").param("page", "-1").param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1));
    }

    @Test
    void leaderboardRanksByBestCompletedSessionAndIgnoresIncompleteSessions() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        List<ArenaRound> rounds = createIsolatedRounds(suffix, 3);

        // improver: first completed session 1/2, second completed session 2/2.
        // The best (2/2) must win over both the earlier session and an
        // incomplete 3/3 session, which must not count at all.
        AppUser improver = registerUser("lb_improver_" + suffix);
        completedSession(improver, rounds, 2, 1);
        completedSession(improver, rounds, 2, 2);
        incompleteSession(improver, rounds, 3, 3);

        // runnerUp: same best correct count (2) but out of 3 answers, so the
        // accuracy tiebreak ranks them below the improver.
        AppUser runnerUp = registerUser("lb_runner_" + suffix);
        completedSession(runnerUp, rounds, 3, 2);

        Map<String, Map<String, Object>> entries = fetchAllEntries();

        Map<String, Object> improverEntry = entries.get(improver.getUsername());
        assertNotNull(improverEntry, "improver must appear on the leaderboard");
        assertEquals(2, ((Number) improverEntry.get("bestSessionCorrect")).intValue());
        assertEquals(2, ((Number) improverEntry.get("bestSessionAnswered")).intValue());
        assertEquals(1.0, ((Number) improverEntry.get("bestSessionAccuracy")).doubleValue(), 1e-9);

        Map<String, Object> runnerUpEntry = entries.get(runnerUp.getUsername());
        assertNotNull(runnerUpEntry, "runner-up must appear on the leaderboard");
        assertEquals(2, ((Number) runnerUpEntry.get("bestSessionCorrect")).intValue());
        assertEquals(3, ((Number) runnerUpEntry.get("bestSessionAnswered")).intValue());

        assertTrue(((Number) improverEntry.get("rank")).intValue()
                        < ((Number) runnerUpEntry.get("rank")).intValue(),
                "equal bestSessionCorrect must rank by accuracy: improver before runner-up");
    }

    @Test
    void leaderboardBreaksFullTiesByUsername() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        List<ArenaRound> rounds = createIsolatedRounds(suffix, 2);

        AppUser alpha = registerUser("lb_tie_a_" + suffix);
        AppUser beta = registerUser("lb_tie_b_" + suffix);
        completedSession(beta, rounds, 2, 1);
        completedSession(alpha, rounds, 2, 1);

        Map<String, Map<String, Object>> entries = fetchAllEntries();
        Map<String, Object> alphaEntry = entries.get(alpha.getUsername());
        Map<String, Object> betaEntry = entries.get(beta.getUsername());
        assertNotNull(alphaEntry);
        assertNotNull(betaEntry);
        assertTrue(((Number) alphaEntry.get("rank")).intValue() < ((Number) betaEntry.get("rank")).intValue(),
                "identical best sessions must order by username");
    }

    @Test
    void leaderboardOmitsUsersWithoutCompletedSessions() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        List<ArenaRound> rounds = createIsolatedRounds(suffix, 2);

        AppUser unfinished = registerUser("lb_unfinished_" + suffix);
        incompleteSession(unfinished, rounds, 2, 2);

        Map<String, Map<String, Object>> entries = fetchAllEntries();
        assertNull(entries.get(unfinished.getUsername()),
                "users with only incomplete sessions must not appear");
    }

    // Walks every leaderboard page (size 50) and returns one map per username,
    // each annotated with its global rank, so assertions are robust against
    // unrelated rows already present in the shared dev database.
    private Map<String, Map<String, Object>> fetchAllEntries() throws Exception {
        Map<String, Map<String, Object>> byUsername = new java.util.HashMap<>();
        int page = 0;
        int totalPages;
        int rank = 0;
        do {
            String json = mockMvc.perform(get("/api/leaderboard")
                            .param("page", Integer.toString(page))
                            .param("size", "50"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            totalPages = ((Number) JsonPath.read(json, "$.totalPages")).intValue();
            List<Map<String, Object>> entries = JsonPath.read(json, "$.entries");
            for (Map<String, Object> entry : entries) {
                Map<String, Object> annotated = new java.util.HashMap<>(entry);
                annotated.put("rank", rank++);
                byUsername.put((String) entry.get("username"), annotated);
            }
            page++;
        } while (page < totalPages);
        return byUsername;
    }

    private AppUser registerUser(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s@example.test","password":"password123"}
                                """.formatted(username, username)))
                .andExpect(status().isCreated());
        return appUserRepository.findByUsername(username).orElseThrow();
    }

    private void completedSession(AppUser user, List<ArenaRound> rounds, int answered, int correct) {
        GameSession session = session(user, rounds, answered, correct);
        session.complete();
        gameSessionRepository.save(session);
    }

    private void incompleteSession(AppUser user, List<ArenaRound> rounds, int answered, int correct) {
        session(user, rounds, answered, correct);
    }

    private GameSession session(AppUser user, List<ArenaRound> rounds, int answered, int correct) {
        GameSession session = gameSessionRepository.save(new GameSession(
                user, ConditionName.TEXT_ONLY, rounds.get(0).getDifficultyLevel()));
        for (int index = 0; index < answered; index++) {
            ArenaRound round = rounds.get(index);
            playerAnswerRepository.save(new PlayerAnswer(
                    session, round, round.getCorrectIdeophone(), round.getCorrectIdeophone(), 500, index < correct));
        }
        return session;
    }

    private List<ArenaRound> createIsolatedRounds(String suffix, int count) {
        int difficulty = Math.toIntExact(300_000L + (System.nanoTime() % 1_000_000L));
        List<ArenaRound> rounds = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            String tag = "lb" + index;
            String prompt = tag + " target " + suffix;
            Ideophone correct = ideophone(tag + "l", suffix, prompt);
            Ideophone distractor = ideophone(tag + "r", suffix, tag + " distractor " + suffix);
            rounds.add(arenaRoundRepository.save(new ArenaRound(
                    prompt, correct, distractor, correct, ConditionName.TEXT_ONLY, difficulty)));
        }
        return rounds;
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
