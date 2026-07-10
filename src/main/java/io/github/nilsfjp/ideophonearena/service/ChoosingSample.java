package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.model.DerivedRound;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

// The single owner of "which of a Meaning Match session's derived rounds are actually
// served" (NIL-85). A full session used to serve the whole scored pool; it now serves a
// deterministic stratified sample of ROUNDS_PER_MODALITY rounds per modality.
//
// The sample is a FILTER over the already-derived rounds, never a re-derivation. That is
// what keeps the frozen shuffle contract (RoundShuffler's class comment) untouched:
// deriveScoredRounds still shuffles the whole pool and draws targetIsPairSecond /
// targetOnLeft / targetMeaningListedFirst per trial off the +0 stream, so every round this
// class keeps carries byte-identical draws to the ones the full derivation gave it. Sampling
// only decides which of those rounds the player sees. Shuffling a pre-truncated pool would
// instead be a different permutation with different draws -- that is the thing not to do.
//
// Stratifying (rather than taking a plain prefix) keeps every session at the same modality
// mix. Modalities differ in difficulty (see pairings.thesis_accuracy), so an unstratified
// prefix would make a player's score partly a function of which modalities their seed drew,
// which is exactly the comparability the leaderboard rests on.
@Component
public class ChoosingSample {

    // 7 auditory + 7 visual + 7 interoceptive = 21 scored rounds (NIL-85, ruled 2026-07-10).
    // The scored CHOOSING pool is 16/16/15, so every modality can supply its 7; the pool
    // floor is guarded by IdeophoneSeedIntegrityTests rather than enforced here, because
    // GameServiceTests exercises this class against small mocked pools.
    static final int ROUNDS_PER_MODALITY = 7;

    // Walks the seeded shuffle in order and keeps the first ROUNDS_PER_MODALITY rounds of
    // each modality, preserving shuffle order. The result is therefore a subsequence of the
    // full derivation: same seed -> same subset and same order; different seed -> a
    // different draw. A modality with fewer than ROUNDS_PER_MODALITY rounds available
    // contributes all of them (Math.min semantics, as practice serving already does), so a
    // short pool yields a short session rather than an exception.
    //
    // HAPTIC never reaches here: findScoredChoosingTrials() excludes it (it is ladder-only).
    // If it ever stopped doing so, this class would sample it like any other modality.
    public List<DerivedRound> sample(List<DerivedRound> derivedInShuffleOrder) {
        Map<Modality, Integer> takenPerModality = new EnumMap<>(Modality.class);
        List<DerivedRound> served = new ArrayList<>();
        for (DerivedRound round : derivedInShuffleOrder) {
            Modality modality = round.getTrial().getPairing().getModality();
            int taken = takenPerModality.getOrDefault(modality, 0);
            if (taken < ROUNDS_PER_MODALITY) {
                served.add(round);
                takenPerModality.put(modality, taken + 1);
            }
        }
        return served;
    }
}
