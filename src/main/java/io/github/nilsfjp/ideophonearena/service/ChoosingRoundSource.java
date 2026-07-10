package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.model.DerivedRound;
import io.github.nilsfjp.ideophonearena.model.GameSession;
import io.github.nilsfjp.ideophonearena.model.Trial;
import io.github.nilsfjp.ideophonearena.model.enums.GameMode;
import io.github.nilsfjp.ideophonearena.repository.TrialRepository;
import java.util.List;
import org.springframework.stereotype.Component;

// Meaning Match serving: the 47 A/V/I scored trials (HAPTIC excluded -- it is ladder-only),
// shuffled by the session seed on the frozen +0 stream, then sampled down to the rounds this
// session actually serves (NIL-85). Derivation and sampling are kept apart on purpose:
// RoundShuffler derives all 47 exactly as it always has, and ChoosingSample decides which of
// them are served -- so the frozen derivation contract holds and a served round's
// target/side/meaning-order draws are unchanged.
@Component
public class ChoosingRoundSource implements RoundSource {

    private final TrialRepository trialRepository;
    private final RoundShuffler roundShuffler;
    private final ChoosingSample choosingSample;

    public ChoosingRoundSource(TrialRepository trialRepository, RoundShuffler roundShuffler,
            ChoosingSample choosingSample) {
        this.trialRepository = trialRepository;
        this.roundShuffler = roundShuffler;
        this.choosingSample = choosingSample;
    }

    @Override
    public GameMode mode() {
        return GameMode.CHOOSING;
    }

    @Override
    public List<DerivedRound> scoredRounds(GameSession session) {
        List<Trial> trials = trialRepository.findScoredChoosingTrials();
        return choosingSample.sample(roundShuffler.deriveScoredRounds(session.getShuffleSeed(), trials));
    }
}
