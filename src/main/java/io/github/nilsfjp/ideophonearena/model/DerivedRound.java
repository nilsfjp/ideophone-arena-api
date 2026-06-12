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

    // Currently unused by the frontend (it always lists the target meaning
    // first); drawn anyway so the derivation spec is final. See the
    // contract's derivation section.
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
