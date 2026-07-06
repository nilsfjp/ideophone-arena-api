package io.github.nilsfjp.ideophonearena.model;

// One trial as a specific session presents it: which pair member is the target,
// which side it sits on, and whether its meaning is listed first. Derived from
// the session's shuffle seed on every request, never persisted.
public final class DerivedRound {

    private final Trial trial;
    private final Word target;
    private final Word other;
    private final boolean targetOnLeft;
    private final boolean targetMeaningListedFirst;

    public DerivedRound(Trial trial, Word target, Word other, boolean targetOnLeft,
            boolean targetMeaningListedFirst) {
        this.trial = trial;
        this.target = target;
        this.other = other;
        this.targetOnLeft = targetOnLeft;
        this.targetMeaningListedFirst = targetMeaningListedFirst;
    }

    public Trial getTrial() {
        return trial;
    }

    public Word getTarget() {
        return target;
    }

    public Word getOther() {
        return other;
    }

    public boolean isTargetOnLeft() {
        return targetOnLeft;
    }

    // Whether the target's meaning is listed first (top) of the two stacked
    // meaning lines -- an independent seed draw, dissociated from the target's
    // left/right card position so a "top line goes with left card" strategy
    // can't leak. Exposed on the round DTO and honored by the frontend since
    // 2026-07-03. See the contract's derivation section.
    public boolean isTargetMeaningListedFirst() {
        return targetMeaningListedFirst;
    }

    public Word getLeft() {
        return targetOnLeft ? target : other;
    }

    public Word getRight() {
        return targetOnLeft ? other : target;
    }
}
