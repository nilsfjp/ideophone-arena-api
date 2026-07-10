package io.github.nilsfjp.ideophonearena.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.nilsfjp.ideophonearena.model.DerivedRound;
import io.github.nilsfjp.ideophonearena.model.Pairing;
import io.github.nilsfjp.ideophonearena.model.Trial;
import io.github.nilsfjp.ideophonearena.model.Word;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

// NIL-85. The sampling contract: a Meaning Match session serves a deterministic,
// modality-stratified subsequence of the frozen full derivation.
class ChoosingSampleTests {

    private static final int PER_MODALITY = ChoosingSample.ROUNDS_PER_MODALITY;

    private final RoundShuffler roundShuffler = new RoundShuffler();
    private final ChoosingSample choosingSample = new ChoosingSample();

    @Test
    void servesExactlySevenRoundsOfEachModalityFromTheLivePoolShape() {
        // The live scored CHOOSING pool: 16 auditory / 16 visual / 15 interoceptive.
        List<Trial> pool = livePoolShape();

        List<DerivedRound> served = sample(4242L, pool);

        assertEquals(3 * PER_MODALITY, served.size(), "a session serves 7+7+7 = 21 scored rounds");
        Map<Modality, Integer> perModality = countByModality(served);
        assertEquals(PER_MODALITY, perModality.get(Modality.AUDITORY));
        assertEquals(PER_MODALITY, perModality.get(Modality.VISUAL));
        assertEquals(PER_MODALITY, perModality.get(Modality.INTEROCEPTIVE));
    }

    @Test
    void everySeedServesTheSameStratifiedShapeFromTheLivePool() {
        List<Trial> pool = livePoolShape();

        // The mix is a property of the sample, not of the draw: no seed can hand a player
        // a modality-skewed session the way an unstratified prefix would.
        for (long seed = -20L; seed <= 20L; seed++) {
            Map<Modality, Integer> perModality = countByModality(sample(seed, pool));
            assertEquals(PER_MODALITY, perModality.get(Modality.AUDITORY), "seed " + seed);
            assertEquals(PER_MODALITY, perModality.get(Modality.VISUAL), "seed " + seed);
            assertEquals(PER_MODALITY, perModality.get(Modality.INTEROCEPTIVE), "seed " + seed);
        }
    }

    @Test
    void sameSeedServesTheSameSubsetInTheSameOrder() {
        List<Trial> pool = livePoolShape();

        String first = signature(sample(7L, pool));
        String second = signature(sample(7L, pool));
        String fromFreshInstances = signature(
                new ChoosingSample().sample(new RoundShuffler().deriveScoredRounds(7L, pool)));

        assertEquals(first, second);
        assertEquals(first, fromFreshInstances);
    }

    @Test
    void differentSeedsServeDifferentSessions() {
        List<Trial> pool = livePoolShape();

        assertNotEquals(signature(sample(1L, pool)), signature(sample(2L, pool)));

        // Not merely a reordering of one fixed subset: the trial membership itself varies.
        Set<String> distinctMemberships = new LinkedHashSet<>();
        for (long seed = 0L; seed < 25L; seed++) {
            distinctMemberships.add(trialIdSet(sample(seed, pool)).toString());
        }
        assertTrue(distinctMemberships.size() > 1,
                "seeds must draw different trials, not just different orders");
    }

    // The load-bearing property. Sampling filters the full derivation; it never re-derives.
    // So each served round sits in the full derivation, in the same relative order, carrying
    // the identical target / side / meaning-order draws. This is what lets the frozen shuffle
    // contract stay untouched, and what lets the position-bias replay resolve every persisted
    // answer against the full derivation.
    @Test
    void servedRoundsAreASubsequenceOfTheFullDerivationWithUnchangedDraws() {
        List<Trial> pool = livePoolShape();
        long seed = 20260710L;

        List<DerivedRound> full = roundShuffler.deriveScoredRounds(seed, pool);
        List<DerivedRound> served = choosingSample.sample(full);

        int fullIndex = 0;
        for (DerivedRound servedRound : served) {
            while (fullIndex < full.size()
                    && !full.get(fullIndex).getTrial().getId().equals(servedRound.getTrial().getId())) {
                fullIndex++;
            }
            assertTrue(fullIndex < full.size(),
                    "served trial " + servedRound.getTrial().getId()
                            + " must appear in the full derivation, in shuffle order");
            assertEquals(signature(List.of(full.get(fullIndex))), signature(List.of(servedRound)),
                    "a served round must carry the draws the full derivation gave it");
            fullIndex++;
        }
    }

    @Test
    void aModalityShorterThanTheQuotaContributesEveryRoundItHas() {
        // Mirrors the practice-serving Math.min: a short pool yields a short session rather
        // than an exception. GameServiceTests leans on this with its small mocked pools.
        List<Trial> pool = new ArrayList<>();
        pool.addAll(trials(Modality.AUDITORY, 2, 0));
        pool.addAll(trials(Modality.VISUAL, PER_MODALITY + 3, 100));

        List<DerivedRound> served = sample(3L, pool);

        Map<Modality, Integer> perModality = countByModality(served);
        assertEquals(2, perModality.get(Modality.AUDITORY));
        assertEquals(PER_MODALITY, perModality.get(Modality.VISUAL));
        assertEquals(2 + PER_MODALITY, served.size());
    }

    @Test
    void anEmptyDerivationServesNothing() {
        assertEquals(List.of(), choosingSample.sample(List.of()));
    }

    private List<DerivedRound> sample(long seed, List<Trial> pool) {
        return choosingSample.sample(roundShuffler.deriveScoredRounds(seed, pool));
    }

    private List<Trial> livePoolShape() {
        List<Trial> pool = new ArrayList<>();
        pool.addAll(trials(Modality.AUDITORY, 16, 0));
        pool.addAll(trials(Modality.VISUAL, 16, 100));
        pool.addAll(trials(Modality.INTEROCEPTIVE, 15, 200));
        return pool;
    }

    private Map<Modality, Integer> countByModality(List<DerivedRound> rounds) {
        Map<Modality, Integer> counts = new EnumMap<>(Modality.class);
        for (DerivedRound round : rounds) {
            Modality modality = round.getTrial().getPairing().getModality();
            counts.merge(modality, 1, Integer::sum);
        }
        return counts;
    }

    private Set<Long> trialIdSet(List<DerivedRound> rounds) {
        Set<Long> ids = new HashSet<>();
        rounds.forEach(round -> ids.add(round.getTrial().getId()));
        return ids;
    }

    // Trial order, target identity, side, and meaning order -- the same signature
    // RoundShufflerTests compares derivations by.
    private String signature(List<DerivedRound> derived) {
        StringBuilder builder = new StringBuilder();
        for (DerivedRound round : derived) {
            builder.append(round.getTrial().getId())
                    .append(':')
                    .append(round.getTarget().getId())
                    .append(':')
                    .append(round.isTargetOnLeft() ? 'L' : 'R')
                    .append(':')
                    .append(round.isTargetMeaningListedFirst() ? 'T' : 'O')
                    .append(';');
        }
        return builder.toString();
    }

    private List<Trial> trials(Modality modality, int count, int idOffset) {
        List<Trial> trials = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            int ordinal = idOffset + index;
            Word wordA = word(1000L + ordinal * 2L, modality, "word" + (ordinal * 2));
            Word wordB = word(1000L + ordinal * 2L + 1, modality, "word" + (ordinal * 2 + 1));
            Trial trial = new Trial(pairing(modality, wordA, wordB), wordA, false);
            setId(trial, 100L + ordinal);
            trials.add(trial);
        }
        return trials;
    }

    private Pairing pairing(Modality modality, Word wordA, Word wordB) {
        return new Pairing("code", null, wordA, wordB, modality, true, "THESIS");
    }

    private Word word(long id, Modality modality, String name) {
        Word word = new Word(null, name, name, name, "H", "meaning of " + name, modality,
                "audio/" + name + ".m4a");
        setId(word, id);
        return word;
    }

    private void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new IllegalStateException("Could not set test id", exception);
        }
    }
}
