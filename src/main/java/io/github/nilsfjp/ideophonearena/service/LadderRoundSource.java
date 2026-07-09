package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.model.DerivedRound;
import io.github.nilsfjp.ideophonearena.model.GameSession;
import io.github.nilsfjp.ideophonearena.model.enums.GameMode;
import java.util.List;
import org.springframework.stereotype.Component;

// Perception Ladder serving: a floor's trials in the fixed easy->hard map order, side and
// identity drawn from the reserved +4 stream (never shuffled -- the order IS the difficulty
// signal). Which trials a floor serves is LadderTrials' single predicate, shared with session
// validation so serving and validation can never disagree.
@Component
public class LadderRoundSource implements RoundSource {

    private final LadderTrials ladderTrials;
    private final RoundShuffler roundShuffler;

    public LadderRoundSource(LadderTrials ladderTrials, RoundShuffler roundShuffler) {
        this.ladderTrials = ladderTrials;
        this.roundShuffler = roundShuffler;
    }

    @Override
    public GameMode mode() {
        return GameMode.LADDER;
    }

    @Override
    public List<DerivedRound> scoredRounds(GameSession session) {
        return roundShuffler.deriveLadderRounds(session.getShuffleSeed(),
                ladderTrials.orderedFloorTrials(session.getLadderFloor()));
    }
}
