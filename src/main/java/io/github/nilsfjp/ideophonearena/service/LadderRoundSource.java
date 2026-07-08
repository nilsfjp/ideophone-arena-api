package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.model.DerivedRound;
import io.github.nilsfjp.ideophonearena.model.GameSession;
import io.github.nilsfjp.ideophonearena.model.Trial;
import io.github.nilsfjp.ideophonearena.model.enums.GameMode;
import io.github.nilsfjp.ideophonearena.repository.TrialRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

// Perception Ladder serving: a floor's trials in the fixed easy->hard map order, side and
// identity drawn from the reserved +4 stream (never shuffled -- the order IS the difficulty
// signal). Trials are fetched by the floor's pair codes (LadderFloors) so the co-modal live
// expansion pairs never enter a floor; codes without a served trial are skipped.
@Component
public class LadderRoundSource implements RoundSource {

    private final TrialRepository trialRepository;
    private final RoundShuffler roundShuffler;
    private final LadderFloors ladderFloors;

    public LadderRoundSource(TrialRepository trialRepository, RoundShuffler roundShuffler, LadderFloors ladderFloors) {
        this.trialRepository = trialRepository;
        this.roundShuffler = roundShuffler;
        this.ladderFloors = ladderFloors;
    }

    @Override
    public GameMode mode() {
        return GameMode.LADDER;
    }

    @Override
    public List<DerivedRound> scoredRounds(GameSession session) {
        List<String> orderedCodes = ladderFloors.pairCodesInOrder(session.getLadderFloor());
        Map<String, Trial> byPairCode = new HashMap<>();
        for (Trial trial : trialRepository.findScoredTrialsByPairCodes(orderedCodes)) {
            byPairCode.put(trial.getPairing().getPairCode(), trial);
        }
        List<Trial> orderedTrials = orderedCodes.stream()
                .map(byPairCode::get)
                .filter(trial -> trial != null)
                .toList();
        return roundShuffler.deriveLadderRounds(session.getShuffleSeed(), orderedTrials);
    }
}
