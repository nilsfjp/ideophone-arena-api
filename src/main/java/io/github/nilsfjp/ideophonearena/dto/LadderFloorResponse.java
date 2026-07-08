package io.github.nilsfjp.ideophonearena.dto;

import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import java.util.List;

// One floor of the Perception Ladder. The enclosing array's position is the floor ordinal
// (28B derives "FLOOR n" from it -- ordinals are never persisted). `pairs` is the floor's
// easy->hard climb order (thesis-facts section 8); `finalRungPairCode` is the last rung. The
// progress fields are the caller's own: `cleared` iff a completed LADDER session for this
// floor exists, with the best session's score. bestCorrect/bestAnswered are null when uncleared.
public class LadderFloorResponse {

    private Modality modality;
    private int pairCount;
    private String finalRungPairCode;
    private boolean cleared;
    private Integer bestCorrect;
    private Integer bestAnswered;
    private List<LadderPairResponse> pairs;

    public LadderFloorResponse(Modality modality, int pairCount, String finalRungPairCode, boolean cleared,
            Integer bestCorrect, Integer bestAnswered, List<LadderPairResponse> pairs) {
        this.modality = modality;
        this.pairCount = pairCount;
        this.finalRungPairCode = finalRungPairCode;
        this.cleared = cleared;
        this.bestCorrect = bestCorrect;
        this.bestAnswered = bestAnswered;
        this.pairs = pairs;
    }

    public Modality getModality() {
        return modality;
    }

    public int getPairCount() {
        return pairCount;
    }

    public String getFinalRungPairCode() {
        return finalRungPairCode;
    }

    public boolean isCleared() {
        return cleared;
    }

    public Integer getBestCorrect() {
        return bestCorrect;
    }

    public Integer getBestAnswered() {
        return bestAnswered;
    }

    public List<LadderPairResponse> getPairs() {
        return pairs;
    }
}
