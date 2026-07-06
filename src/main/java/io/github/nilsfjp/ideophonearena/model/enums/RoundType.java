package io.github.nilsfjp.ideophonearena.model.enums;

// The task shape a trial serves (ADR-3). CHOOSING is the core 2AFC word-pick;
// TEMPLATE and XL arrive with their build sessions. Cross-round_type pooling of
// stats is forbidden (different chance structures).
public enum RoundType {
    CHOOSING,
    TEMPLATE,
    XL
}
