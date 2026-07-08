package io.github.nilsfjp.ideophonearena.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * NIL-54 Rider A extension. Usernames partition the population into three
 * disjoint cohorts: browser_loop_% automation, thesis_p% ingestion, and everyone
 * else. The live research aggregates must count only the third. This proves that
 * algebraically -- raw = rider + thesis + browserLoop for every word -- which
 * holds no matter how much live-player data other (shared, non-rolled-back) tests
 * have written, so it never flakes on ordering.
 */
@SpringBootTest
@Transactional
class ThesisCohortExclusionTests {

    @Autowired
    private PlayerAnswerRepository playerAnswerRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void liveGuessAggregateExcludesExactlyTheThesisAndBrowserLoopCohorts() {
        Map<Long, Long> rider = guessMap(playerAnswerRepository.aggregateGuessStatsByWord());
        Map<Long, Long> thesis = guessMap(playerAnswerRepository.aggregateThesisGuessStatsByWord());
        // raw stays the full non-practice universe so LADDER-target words (e.g. the HAPTIC
        // floor, which has no CHOOSING answers at all) remain in raw.keySet() and under
        // scrutiny -- narrowing raw to CHOOSING would drop them and blind this check to a
        // research aggregate that ever starts leaking LADDER answers for a ladder-only word.
        // The research aggregates (rider, thesis) are CHOOSING-only by design (NIL-41);
        // LADDER answers are a deliberately-excluded category, so the partition carries an
        // explicit ladder (non-CHOOSING) term. browserLoop is scoped to CHOOSING so every
        // cell is disjoint (a browser_loop LADDER answer belongs to ladder, not browserLoop):
        //   raw = rider (CHOOSING non-cohort) + thesis (CHOOSING thesis)
        //       + browserLoop (CHOOSING browser_loop) + ladder (all non-CHOOSING).
        Map<Long, Long> raw = countByWord(
                "select word.id, count(answer.id) from PlayerAnswer answer join answer.targetWord word "
                        + "where answer.trial.practice = false group by word.id");
        Map<Long, Long> browserLoop = countByWord(
                "select word.id, count(answer.id) from PlayerAnswer answer join answer.targetWord word "
                        + "where answer.trial.practice = false "
                        + "and answer.session.gameMode = io.github.nilsfjp.ideophonearena.model.enums.GameMode.CHOOSING "
                        + "and answer.session.user.username like 'browser!_loop!_%' escape '!' group by word.id");
        Map<Long, Long> ladder = countByWord(
                "select word.id, count(answer.id) from PlayerAnswer answer join answer.targetWord word "
                        + "where answer.trial.practice = false "
                        + "and answer.session.gameMode <> io.github.nilsfjp.ideophonearena.model.enums.GameMode.CHOOSING "
                        + "group by word.id");

        // The thesis cohort must actually be present (guards against the seed not
        // being loaded, which would make the identity vacuously true).
        assertEquals(30, thesis.size(), "the 30 thesis target words must carry thesis guesses");
        thesis.values().forEach(count -> assertEquals(36L, count, "each thesis target word has 36 guesses"));

        for (Long wordId : raw.keySet()) {
            long expected = rider.getOrDefault(wordId, 0L)
                    + thesis.getOrDefault(wordId, 0L)
                    + browserLoop.getOrDefault(wordId, 0L)
                    + ladder.getOrDefault(wordId, 0L);
            assertEquals(raw.get(wordId), expected,
                    "guess count for word " + wordId + " must partition into CHOOSING rider + thesis "
                            + "+ browser_loop plus a deliberately-excluded non-CHOOSING ladder term");
        }
        // And the live aggregate never carries a thesis-only word.
        for (Long wordId : thesis.keySet()) {
            assertTrue(rider.getOrDefault(wordId, 0L) < raw.get(wordId),
                    "live guess count for word " + wordId + " must drop the 36 thesis guesses");
        }
    }

    @Test
    void liveRatingAggregateExcludesExactlyTheThesisAndBrowserLoopCohorts() {
        Map<Long, Long> rider = ratingMap(ratingRepository.aggregateRatingStatsByWord());
        Map<Long, Long> thesis = ratingMap(ratingRepository.aggregateThesisRatingStatsByWord());
        Map<Long, Long> raw = countByWord(
                "select word.id, count(rating.id) from Rating rating join rating.word word group by word.id");
        Map<Long, Long> browserLoop = countByWord(
                "select word.id, count(rating.id) from Rating rating join rating.word word "
                        + "where rating.user.username like 'browser!_loop!_%' escape '!' group by word.id");

        assertEquals(30, thesis.size(), "the 30 thesis target words must carry thesis ratings");
        thesis.values().forEach(count -> assertEquals(36L, count, "each thesis target word has 36 ratings"));

        for (Long wordId : raw.keySet()) {
            long expected = rider.getOrDefault(wordId, 0L)
                    + thesis.getOrDefault(wordId, 0L)
                    + browserLoop.getOrDefault(wordId, 0L);
            assertEquals(raw.get(wordId), expected,
                    "Rider A rating count for word " + wordId + " must exclude exactly thesis + browser_loop");
        }
    }

    private Map<Long, Long> guessMap(List<IdeophoneGuessStatsProjection> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (IdeophoneGuessStatsProjection row : rows) {
            map.put(row.getIdeophoneId(), row.getGuesses());
        }
        return map;
    }

    private Map<Long, Long> ratingMap(List<IdeophoneRatingStatsProjection> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (IdeophoneRatingStatsProjection row : rows) {
            map.put(row.getIdeophoneId(), row.getRatingCount());
        }
        return map;
    }

    private Map<Long, Long> countByWord(String jpql) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : entityManager.createQuery(jpql, Object[].class).getResultList()) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }
}
