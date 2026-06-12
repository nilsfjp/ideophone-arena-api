package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.model.ArenaRound;
import io.github.nilsfjp.ideophonearena.model.DerivedRound;
import io.github.nilsfjp.ideophonearena.model.Ideophone;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Component;

// Deterministically derives a session's round presentation from its shuffle
// seed. Nothing here is persisted; every request recomputes the same result,
// so a session replays identically across server restarts. The spec below is
// a compatibility contract (documented in docs/backend-contract.md) and must
// not change once real sessions exist:
//
// 1. Scored rounds for the session's condition and difficulty, ordered by
//    round id ascending, are shuffled with
//    Collections.shuffle(list, new Random(shuffleSeed)).
// 2. Iterating the shuffled list in order, three draws per round are taken
//    from the same Random stream, in this order: targetIsPairSecond,
//    targetOnLeft, targetMeaningListedFirst. "Pair second" is the round
//    member with the higher ideophone id.
// 3. Practice rounds keep their fixed seed order; the same three per-round
//    draws come from a separate stream, new Random(shuffleSeed + 1), so the
//    scored derivation is unaffected by practice on/off.
@Component
public class RoundShuffler {

    public List<DerivedRound> deriveScoredRounds(long shuffleSeed, List<ArenaRound> roundsOrderedById) {
        List<ArenaRound> shuffled = new ArrayList<>(roundsOrderedById);
        Random random = new Random(shuffleSeed);
        Collections.shuffle(shuffled, random);
        return drawPresentation(shuffled, random);
    }

    public List<DerivedRound> derivePracticeRounds(long shuffleSeed, List<ArenaRound> practiceRoundsInOrder) {
        return drawPresentation(practiceRoundsInOrder, new Random(shuffleSeed + 1));
    }

    private List<DerivedRound> drawPresentation(List<ArenaRound> rounds, Random random) {
        List<DerivedRound> derived = new ArrayList<>(rounds.size());
        for (ArenaRound round : rounds) {
            boolean targetIsPairSecond = random.nextBoolean();
            boolean targetOnLeft = random.nextBoolean();
            boolean targetMeaningListedFirst = random.nextBoolean();
            Ideophone pairFirst = pairMember(round, true);
            Ideophone pairSecond = pairMember(round, false);
            Ideophone target = targetIsPairSecond ? pairSecond : pairFirst;
            Ideophone other = targetIsPairSecond ? pairFirst : pairSecond;
            derived.add(new DerivedRound(round, target, other, targetOnLeft, targetMeaningListedFirst));
        }
        return derived;
    }

    private Ideophone pairMember(ArenaRound round, boolean first) {
        Ideophone lower = round.getLeftIdeophone();
        Ideophone higher = round.getRightIdeophone();
        if (lower.getId() > higher.getId()) {
            Ideophone swap = lower;
            lower = higher;
            higher = swap;
        }
        return first ? lower : higher;
    }
}
