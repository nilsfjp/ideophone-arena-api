package io.github.nilsfjp.ideophonearena.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.nilsfjp.ideophonearena.model.ArenaRound;
import io.github.nilsfjp.ideophonearena.model.DerivedRound;
import io.github.nilsfjp.ideophonearena.model.Ideophone;
import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RoundShufflerTests {

    private final RoundShuffler roundShuffler = new RoundShuffler();

    @Test
    void sameSeedAndRoundsDeriveIdenticalSequenceOnRepeatedCalls() {
        List<ArenaRound> rounds = rounds(10);

        List<DerivedRound> first = roundShuffler.deriveScoredRounds(7L, rounds);
        List<DerivedRound> second = roundShuffler.deriveScoredRounds(7L, rounds);
        List<DerivedRound> fromFreshInstance = new RoundShuffler().deriveScoredRounds(7L, rounds);

        assertEquals(signature(first), signature(second));
        assertEquals(signature(first), signature(fromFreshInstance));
    }

    @Test
    void differentSeedsDeriveDifferentSequences() {
        List<ArenaRound> rounds = rounds(30);

        List<DerivedRound> seedOne = roundShuffler.deriveScoredRounds(1L, rounds);
        List<DerivedRound> seedTwo = roundShuffler.deriveScoredRounds(2L, rounds);

        assertNotEquals(signature(seedOne), signature(seedTwo));
    }

    @Test
    void derivationShufflesOrderButKeepsTheSameRounds() {
        List<ArenaRound> rounds = rounds(10);

        List<DerivedRound> derived = roundShuffler.deriveScoredRounds(99L, rounds);

        Set<Long> inputIds = new HashSet<>();
        rounds.forEach(round -> inputIds.add(round.getId()));
        Set<Long> derivedIds = new HashSet<>();
        derived.forEach(round -> derivedIds.add(round.getRound().getId()));
        assertEquals(inputIds, derivedIds);
        assertEquals(rounds.size(), derived.size());
    }

    // "Pair second" is defined on ideophone ids, not on the left/right
    // columns: swapping the stored columns must not change which word the
    // seed picks as target.
    @Test
    void targetIdentityIsStableUnderSwappedLeftRightColumns() {
        List<ArenaRound> rounds = rounds(10);
        List<ArenaRound> swapped = new ArrayList<>();
        for (ArenaRound round : rounds) {
            ArenaRound copy = new ArenaRound(
                    round.getPrompt(),
                    round.getRightIdeophone(),
                    round.getLeftIdeophone(),
                    round.getCorrectIdeophone(),
                    round.getConditionName(),
                    round.getDifficultyLevel()
            );
            setId(copy, round.getId());
            swapped.add(copy);
        }

        List<DerivedRound> original = roundShuffler.deriveScoredRounds(5L, rounds);
        List<DerivedRound> derivedFromSwapped = roundShuffler.deriveScoredRounds(5L, swapped);

        assertEquals(signature(original), signature(derivedFromSwapped));
    }

    // Across ~200 seeds, every round's target must take both pair identities
    // and both sides, and both meaning orders must occur: no fixed-point bug.
    @Test
    void acrossManySeedsEveryRoundTakesBothIdentitiesAndBothSides() {
        List<ArenaRound> rounds = rounds(4);
        Map<Long, Set<Long>> targetsPerRound = new HashMap<>();
        Map<Long, Set<Boolean>> sidesPerRound = new HashMap<>();
        Set<Boolean> meaningOrders = new HashSet<>();

        for (long seed = 0; seed < 200; seed++) {
            for (DerivedRound derived : roundShuffler.deriveScoredRounds(seed, rounds)) {
                Long roundId = derived.getRound().getId();
                targetsPerRound.computeIfAbsent(roundId, key -> new HashSet<>()).add(derived.getTarget().getId());
                sidesPerRound.computeIfAbsent(roundId, key -> new HashSet<>()).add(derived.isTargetOnLeft());
                meaningOrders.add(derived.isTargetMeaningListedFirst());
            }
        }

        for (ArenaRound round : rounds) {
            assertEquals(2, targetsPerRound.get(round.getId()).size(),
                    "round " + round.getId() + " target must take both pair identities");
            assertEquals(2, sidesPerRound.get(round.getId()).size(),
                    "round " + round.getId() + " target must appear on both sides");
        }
        assertEquals(2, meaningOrders.size(), "both meaning orders must occur");
    }

    @Test
    void practiceRoundsKeepFixedOrderAndUseAStreamIndependentOfScoredRounds() {
        List<ArenaRound> practice = rounds(2);
        List<ArenaRound> scoredFew = rounds(3);
        List<ArenaRound> scoredMany = rounds(30);

        List<DerivedRound> derivedPractice = roundShuffler.derivePracticeRounds(11L, practice);
        for (int index = 0; index < practice.size(); index++) {
            assertEquals(practice.get(index).getId(), derivedPractice.get(index).getRound().getId(),
                    "practice rounds must keep their input order");
        }
        assertEquals(signature(derivedPractice), signature(roundShuffler.derivePracticeRounds(11L, practice)));

        // The scored derivation must be byte-identical whether or not practice
        // was derived first, and regardless of the scored set's size: the two
        // streams are independent by construction.
        assertEquals(
                signature(roundShuffler.deriveScoredRounds(11L, scoredFew)),
                signature(roundShuffler.deriveScoredRounds(11L, scoredFew))
        );
        roundShuffler.derivePracticeRounds(11L, practice);
        assertEquals(
                signature(roundShuffler.deriveScoredRounds(11L, scoredMany)),
                signature(new RoundShuffler().deriveScoredRounds(11L, scoredMany))
        );
    }

    @Test
    void everyDerivedRoundPairsTargetAndOtherFromTheSameRound() {
        List<ArenaRound> rounds = rounds(10);

        for (DerivedRound derived : roundShuffler.deriveScoredRounds(123L, rounds)) {
            Set<Long> members = Set.of(
                    derived.getRound().getLeftIdeophone().getId(),
                    derived.getRound().getRightIdeophone().getId()
            );
            assertTrue(members.contains(derived.getTarget().getId()));
            assertTrue(members.contains(derived.getOther().getId()));
            assertNotEquals(derived.getTarget().getId(), derived.getOther().getId());
            assertEquals(derived.isTargetOnLeft() ? derived.getTarget() : derived.getOther(), derived.getLeft());
            assertEquals(derived.isTargetOnLeft() ? derived.getOther() : derived.getTarget(), derived.getRight());
        }
    }

    // The derived presentation as a comparable string: round order, target
    // identity, side, and meaning order.
    private String signature(List<DerivedRound> derived) {
        StringBuilder builder = new StringBuilder();
        for (DerivedRound round : derived) {
            builder.append(round.getRound().getId())
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

    private List<ArenaRound> rounds(int count) {
        List<ArenaRound> rounds = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            Ideophone lower = ideophone(1000L + index * 2, "word" + (index * 2));
            Ideophone higher = ideophone(1000L + index * 2 + 1, "word" + (index * 2 + 1));
            ArenaRound round = new ArenaRound(
                    lower.getGloss(),
                    lower,
                    higher,
                    lower,
                    ConditionName.CONDITION_1_SOKUON,
                    1
            );
            setId(round, 100L + index);
            rounds.add(round);
        }
        return rounds;
    }

    private Ideophone ideophone(long id, String name) {
        Ideophone ideophone = new Ideophone(name, name, name, name, "meaning of " + name, "HU",
                "audio/" + name + ".m4a", Modality.AUDITORY);
        setId(ideophone, id);
        return ideophone;
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
