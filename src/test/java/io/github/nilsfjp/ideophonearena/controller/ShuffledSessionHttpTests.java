package io.github.nilsfjp.ideophonearena.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.nilsfjp.ideophonearena.model.AppUser;
import io.github.nilsfjp.ideophonearena.model.ArenaRound;
import io.github.nilsfjp.ideophonearena.model.DerivedRound;
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
import io.github.nilsfjp.ideophonearena.service.RoundShuffler;
import java.util.ArrayList;
import java.util.HashMap;
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
 * Seed-derived shuffle over the wire, against an isolated fixture (unique
 * difficulty, sessions created through the repository so the seed is known):
 * the served order, sides, and target meanings must match the derivation, the
 * stored answer must carry the derived target, and completion and duplicate
 * semantics must be unchanged.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ShuffledSessionHttpTests {

    private static final long SHUFFLE_SEED = 987654321L;

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

    @Autowired
    private PlayerAnswerRepository playerAnswerRepository;

    @Autowired
    private RoundShuffler roundShuffler;

    @Test
    void fullSessionWithPracticeFollowsDerivationAndStoresDerivedTargets() throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String username = "shuffle_http_" + suffix;
        String token = registerAndGetToken(username);
        AppUser user = appUserRepository.findByUsername(username).orElseThrow();
        int difficulty = Math.toIntExact(400_000L + (System.nanoTime() % 1_000_000L));

        List<ArenaRound> practiceRounds = List.of(
                fixtureRound("sp0", suffix, difficulty, true),
                fixtureRound("sp1", suffix, difficulty, true)
        );
        List<ArenaRound> scoredRounds = new ArrayList<>(List.of(
                fixtureRound("ss0", suffix, difficulty, false),
                fixtureRound("ss1", suffix, difficulty, false),
                fixtureRound("ss2", suffix, difficulty, false)
        ));
        scoredRounds.sort((a, b) -> a.getId().compareTo(b.getId()));

        GameSession session = gameSessionRepository.save(new GameSession(
                user, ConditionName.TEXT_ONLY, difficulty, true, SHUFFLE_SEED));
        String sessionUuid = session.getSessionUuid();

        List<DerivedRound> derivedPractice = roundShuffler.derivePracticeRounds(SHUFFLE_SEED, practiceRounds);
        List<DerivedRound> derivedScored = roundShuffler.deriveScoredRounds(SHUFFLE_SEED, scoredRounds);

        for (DerivedRound expected : derivedPractice) {
            String roundJson = getNextRound(token, sessionUuid);
            assertEquals(Boolean.TRUE, JsonPath.read(roundJson, "$.practice"));
            assertServedAsDerived(expected, roundJson);
            String answerJson = submitAnswer(token, sessionUuid, expected.getRound().getId(),
                    expected.getTarget().getId());
            assertEquals(Boolean.TRUE, JsonPath.read(answerJson, "$.practice"));
            assertEquals(Boolean.TRUE, JsonPath.read(answerJson, "$.correct"));
        }

        long answered = 0;
        for (DerivedRound expected : derivedScored) {
            String roundJson = getNextRound(token, sessionUuid);
            assertEquals(Boolean.FALSE, JsonPath.read(roundJson, "$.practice"));
            assertServedAsDerived(expected, roundJson);

            String answerJson = submitAnswer(token, sessionUuid, expected.getRound().getId(),
                    expected.getTarget().getId());
            answered++;
            assertEquals(Boolean.TRUE, JsonPath.read(answerJson, "$.correct"));
            assertEquals(expected.getTarget().getId().longValue(),
                    ((Number) JsonPath.read(answerJson, "$.correctIdeophoneId")).longValue());
            assertEquals(answered, ((Number) JsonPath.read(answerJson, "$.totalAnswered")).longValue());
            assertEquals(answered, ((Number) JsonPath.read(answerJson, "$.totalCorrect")).longValue());

            // Duplicate answers stay 409 under the derived flow.
            mockMvc.perform(post("/api/game/sessions/{sessionUuid}/answers", sessionUuid)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"roundId":%d,"selectedIdeophoneId":%d,"responseTimeMs":456}
                                    """.formatted(expected.getRound().getId(), expected.getTarget().getId())))
                    .andExpect(status().isConflict());
        }

        String completionJson = getNextRound(token, sessionUuid);
        assertEquals(Boolean.TRUE, JsonPath.read(completionJson, "$.completed"));
        assertNotNull(gameSessionRepository.findBySessionUuid(sessionUuid).orElseThrow().getCompletedAt());

        assertStoredTargetsMatchDerivation(session.getId(), derivedScored);
    }

    // The EntityGraph on findBySessionId fetches round and targetIdeophone,
    // so no transaction is needed here. Answers are matched by round id
    // because answered_at only has second precision.
    private void assertStoredTargetsMatchDerivation(Long sessionId, List<DerivedRound> derivedScored) {
        List<PlayerAnswer> storedAnswers = playerAnswerRepository.findBySessionId(sessionId);
        assertEquals(derivedScored.size(), storedAnswers.size());
        Map<Long, Long> storedTargetByRoundId = new HashMap<>();
        for (PlayerAnswer answer : storedAnswers) {
            storedTargetByRoundId.put(answer.getRound().getId(), answer.getTargetIdeophone().getId());
        }
        for (DerivedRound derived : derivedScored) {
            assertEquals(derived.getTarget().getId(), storedTargetByRoundId.get(derived.getRound().getId()),
                    "stored target must match the derivation for round " + derived.getRound().getId());
        }
    }

    private void assertServedAsDerived(DerivedRound expected, String roundJson) {
        assertEquals(expected.getRound().getId().longValue(),
                ((Number) JsonPath.read(roundJson, "$.roundId")).longValue());
        assertEquals(expected.getTarget().getGloss(), JsonPath.read(roundJson, "$.targetTranslation"));
        assertEquals(expected.getTarget().getGloss(), JsonPath.read(roundJson, "$.translations.target"));
        assertEquals(expected.getOther().getGloss(), JsonPath.read(roundJson, "$.translations.other"));
        assertEquals(expected.getLeft().getId().longValue(),
                ((Number) JsonPath.read(roundJson, "$.left.ideophoneId")).longValue());
        assertEquals(expected.getRight().getId().longValue(),
                ((Number) JsonPath.read(roundJson, "$.right.ideophoneId")).longValue());
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

    private String getNextRound(String token, String sessionUuid) throws Exception {
        return mockMvc.perform(get("/api/game/sessions/{sessionUuid}/rounds/next", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String submitAnswer(String token, String sessionUuid, Long roundId, Long selectedIdeophoneId)
            throws Exception {
        return mockMvc.perform(post("/api/game/sessions/{sessionUuid}/answers", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundId":%d,"selectedIdeophoneId":%d,"responseTimeMs":456}
                                """.formatted(roundId, selectedIdeophoneId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
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
