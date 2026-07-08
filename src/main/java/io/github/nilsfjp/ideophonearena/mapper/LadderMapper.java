package io.github.nilsfjp.ideophonearena.mapper;

import io.github.nilsfjp.ideophonearena.dto.LadderFloorResponse;
import io.github.nilsfjp.ideophonearena.dto.LadderFloorsResponse;
import io.github.nilsfjp.ideophonearena.dto.LadderPairResponse;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

// Builds the Perception Ladder overview DTOs. Floor selection/order and best-score
// resolution stay in LadderService; this mapper only turns the resolved pieces into
// response shapes. pairCount and finalRungPairCode are both derived from presentCodes
// here, so the two are consistent by construction.
@Component
public class LadderMapper {

    public LadderFloorsResponse toFloorsResponse(List<LadderFloorResponse> floors) {
        return new LadderFloorsResponse(floors);
    }

    // presentCodes is the floor's easy->hard order (last = final rung). cleared plus the
    // nullable best* scores are the caller's own progress, resolved by the service.
    public LadderFloorResponse toFloorResponse(Modality modality, List<String> presentCodes,
            boolean cleared, Integer bestCorrect, Integer bestAnswered) {
        String finalRungPairCode = presentCodes.get(presentCodes.size() - 1);
        List<LadderPairResponse> pairs = new ArrayList<>(presentCodes.size());
        for (String pairCode : presentCodes) {
            pairs.add(new LadderPairResponse(pairCode, pairCode.equals(finalRungPairCode)));
        }
        return new LadderFloorResponse(modality, presentCodes.size(), finalRungPairCode, cleared,
                bestCorrect, bestAnswered, pairs);
    }
}
