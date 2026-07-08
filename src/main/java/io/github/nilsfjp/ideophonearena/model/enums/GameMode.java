package io.github.nilsfjp.ideophonearena.model.enums;

// The session's play mode (M1, ADR-2). CHOOSING is the core Meaning Match loop and the
// default for every legacy session; LADDER is the Perception Ladder (floor-scoped serving,
// NIL-41). TEMPLATE_READING and CROSS_LINGUISTIC are reserved Phase-2 modes, not yet built.
public enum GameMode {
    CHOOSING,
    LADDER,
    TEMPLATE_READING,
    CROSS_LINGUISTIC
}
