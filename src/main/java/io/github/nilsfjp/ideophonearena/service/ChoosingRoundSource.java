package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.model.DerivedRound;
import io.github.nilsfjp.ideophonearena.model.GameSession;
import io.github.nilsfjp.ideophonearena.model.Trial;
import io.github.nilsfjp.ideophonearena.model.enums.GameMode;
import io.github.nilsfjp.ideophonearena.repository.TrialRepository;
import java.util.List;
import org.springframework.stereotype.Component;

// Meaning Match serving: the 47 A/V/I scored trials (HAPTIC excluded -- it is ladder-only),
// shuffled by the session seed on the frozen +0 stream. This is the pre-NIL-41 behavior,
// unchanged.
@Component
public class ChoosingRoundSource implements RoundSource {

    private final TrialRepository trialRepository;
    private final RoundShuffler roundShuffler;

    public ChoosingRoundSource(TrialRepository trialRepository, RoundShuffler roundShuffler) {
        this.trialRepository = trialRepository;
        this.roundShuffler = roundShuffler;
    }

    @Override
    public GameMode mode() {
        return GameMode.CHOOSING;
    }

    @Override
    public List<DerivedRound> scoredRounds(GameSession session) {
        List<Trial> trials = trialRepository.findScoredChoosingTrials();
        return roundShuffler.deriveScoredRounds(session.getShuffleSeed(), trials);
    }
}
