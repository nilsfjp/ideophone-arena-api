package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.model.Trial;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import io.github.nilsfjp.ideophonearena.repository.TrialRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

// The single owner of "which trials a ladder floor serves". Session validation and round
// serving both ask this one predicate, so a floor can never pass the start check and then
// serve zero rounds -- the divergence that existed while validation only asked whether the
// pair-code query returned anything, and serving additionally keyed trials by pair code.
@Component
public class LadderTrials {

    private final TrialRepository trialRepository;
    private final LadderFloors ladderFloors;

    public LadderTrials(TrialRepository trialRepository, LadderFloors ladderFloors) {
        this.trialRepository = trialRepository;
        this.ladderFloors = ladderFloors;
    }

    // The floor's served trials in the fixed easy->hard map order. Empty when the modality is
    // not a ladder floor, and empty when none of its pair codes has a seeded scored trial:
    // trials are keyed by the floor's codes (LadderFloors), so a code without a served trial
    // is skipped and a trial under an unexpected code never enters the floor.
    public List<Trial> orderedFloorTrials(Modality floor) {
        List<String> orderedCodes = ladderFloors.pairCodesInOrder(floor);
        if (orderedCodes.isEmpty()) {
            return List.of();
        }
        Map<String, Trial> byPairCode = new HashMap<>();
        for (Trial trial : trialRepository.findScoredTrialsByPairCodes(orderedCodes)) {
            byPairCode.put(trial.getPairing().getPairCode(), trial);
        }
        return orderedCodes.stream()
                .map(byPairCode::get)
                .filter(Objects::nonNull)
                .toList();
    }

    // A floor is servable exactly when it serves at least one trial. Data-driven: a floor
    // whose trials are not yet seeded is rejected at session start rather than serving an
    // empty session.
    public boolean isServedFloor(Modality floor) {
        return !orderedFloorTrials(floor).isEmpty();
    }
}
