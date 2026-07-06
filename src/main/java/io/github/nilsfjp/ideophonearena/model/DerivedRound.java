package io.github.nilsfjp.ideophonearena.model;

// One round as a specific session presents it: which pair member is the
// target, which side it sits on, and whether its meaning is listed first.
// Derived from the session's shuffle seed on every request, never persisted.
public final class DerivedRound {

    private final ArenaRound round;
    private final Ideophone target;
    private final Ideophone other;
    private final boolean targetOnLeft;
    private final boolean targetMeaningListedFirst;

    public DerivedRound(ArenaRound round, Ideophone target, Ideophone other, boolean targetOnLeft,
            boolean targetMeaningListedFirst) {
        this.round = round;
        this.target = target;
        this.other = other;
        this.targetOnLeft = targetOnLeft;
        this.targetMeaningListedFirst = targetMeaningListedFirst;
    }

    public ArenaRound getRound() {
        return round;
    }

    public Ideophone getTarget() {
        return target;
    }

    public Ideophone getOther() {
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

    public Ideophone getLeft() {
        return targetOnLeft ? target : other;
    }

    public Ideophone getRight() {
        return targetOnLeft ? other : target;
    }
}
