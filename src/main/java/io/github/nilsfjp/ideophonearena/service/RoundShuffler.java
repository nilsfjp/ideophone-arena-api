package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.model.DerivedRound;
import io.github.nilsfjp.ideophonearena.model.Trial;
import io.github.nilsfjp.ideophonearena.model.Word;
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
// 1. Scored trials, ordered by trial id ascending, are shuffled with
//    Collections.shuffle(list, new Random(shuffleSeed)).
// 2. Iterating the shuffled list in order, three draws per trial are taken
//    from the same Random stream, in this order: targetIsPairSecond,
//    targetOnLeft, targetMeaningListedFirst. "Pair second" is the trial's
//    pairing member with the higher WORD id (M2 re-key: word ids assigned in
//    CSV order put word_a before word_b, so this is the same k-word member the
//    pre-M2 "higher ideophone id" rule selected -- the draws are byte-identical).
// 3. Practice trials keep their fixed seed order; the same three per-trial
//    draws come from a separate stream, new Random(shuffleSeed + 1), so the
//    scored derivation is unaffected by practice on/off.
@Component
public class RoundShuffler {

    public List<DerivedRound> deriveScoredRounds(long shuffleSeed, List<Trial> trialsOrderedById) {
        List<Trial> shuffled = new ArrayList<>(trialsOrderedById);
        Random random = new Random(shuffleSeed);
        Collections.shuffle(shuffled, random);
        return drawPresentation(shuffled, random);
    }

    public List<DerivedRound> derivePracticeRounds(long shuffleSeed, List<Trial> practiceTrialsInOrder) {
        return drawPresentation(practiceTrialsInOrder, new Random(shuffleSeed + 1));
    }

    private List<DerivedRound> drawPresentation(List<Trial> trials, Random random) {
        List<DerivedRound> derived = new ArrayList<>(trials.size());
        for (Trial trial : trials) {
            boolean targetIsPairSecond = random.nextBoolean();
            boolean targetOnLeft = random.nextBoolean();
            boolean targetMeaningListedFirst = random.nextBoolean();
            Word pairFirst = pairMember(trial, true);
            Word pairSecond = pairMember(trial, false);
            Word target = targetIsPairSecond ? pairSecond : pairFirst;
            Word other = targetIsPairSecond ? pairFirst : pairSecond;
            derived.add(new DerivedRound(trial, target, other, targetOnLeft, targetMeaningListedFirst));
        }
        return derived;
    }

    private Word pairMember(Trial trial, boolean first) {
        Word lower = trial.getPairing().getWordA();
        Word higher = trial.getPairing().getWordB();
        if (lower.getId() > higher.getId()) {
            Word swap = lower;
            lower = higher;
            higher = swap;
        }
        return first ? lower : higher;
    }
}
