package io.github.nilsfjp.ideophonearena.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// SDT-flavored fairness check on the forced choice, reconstructed from the
// deterministic per-session shuffle (no schema change). Two dissociated axes:
//   - word-card left/right: pick rate vs 0.5, plus d'/criterion on this axis
//     (signal = target on left, response = pick left);
//   - target meaning top/bottom: accuracy conditioned on where the target's
//     meaning line sat -- fair iff the two accuracies match.
// Rates/accuracies are null (not 0.0) when their denominator is 0, so "no data"
// stays distinct from a real 0. d'/criterion are null when a stimulus class is
// empty. NIL-68 later re-points the aggregate without changing this shape.
public class PositionBiasResponse {

    private long n;

    private long leftPickCount;
    private long rightPickCount;
    private Double leftPickRate;

    // Bean introspection would mangle a `dPrime` field to `dprime`; pin the
    // frozen contract name explicitly.
    @JsonProperty("dPrime")
    private Double dPrime;
    private Double criterion;

    private long targetTopN;
    private long targetTopCorrect;
    private Double targetTopAccuracy;

    private long targetBottomN;
    private long targetBottomCorrect;
    private Double targetBottomAccuracy;

    public PositionBiasResponse(long n, long leftPickCount, long rightPickCount, Double leftPickRate,
            Double dPrime, Double criterion,
            long targetTopN, long targetTopCorrect, Double targetTopAccuracy,
            long targetBottomN, long targetBottomCorrect, Double targetBottomAccuracy) {
        this.n = n;
        this.leftPickCount = leftPickCount;
        this.rightPickCount = rightPickCount;
        this.leftPickRate = leftPickRate;
        this.dPrime = dPrime;
        this.criterion = criterion;
        this.targetTopN = targetTopN;
        this.targetTopCorrect = targetTopCorrect;
        this.targetTopAccuracy = targetTopAccuracy;
        this.targetBottomN = targetBottomN;
        this.targetBottomCorrect = targetBottomCorrect;
        this.targetBottomAccuracy = targetBottomAccuracy;
    }

    public long getN() {
        return n;
    }

    public long getLeftPickCount() {
        return leftPickCount;
    }

    public long getRightPickCount() {
        return rightPickCount;
    }

    public Double getLeftPickRate() {
        return leftPickRate;
    }

    @JsonProperty("dPrime")
    public Double getDPrime() {
        return dPrime;
    }

    public Double getCriterion() {
        return criterion;
    }

    public long getTargetTopN() {
        return targetTopN;
    }

    public long getTargetTopCorrect() {
        return targetTopCorrect;
    }

    public Double getTargetTopAccuracy() {
        return targetTopAccuracy;
    }

    public long getTargetBottomN() {
        return targetBottomN;
    }

    public long getTargetBottomCorrect() {
        return targetBottomCorrect;
    }

    public Double getTargetBottomAccuracy() {
        return targetBottomAccuracy;
    }
}
