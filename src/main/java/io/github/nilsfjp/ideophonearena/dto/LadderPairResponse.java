package io.github.nilsfjp.ideophonearena.dto;

// One rung of a Perception Ladder floor, in easy->hard order. Carries only a stable
// pair code and the final-rung marker -- NO per-pair difficulty number and no word
// content pre-answer (V5): the position IS the difficulty signal, and the round content
// is delivered by GET .../rounds/next during play. "FINAL RUNG" is 28B's label; the API
// just marks which rung is last.
public class LadderPairResponse {

    private String pairCode;
    private boolean finalRung;

    public LadderPairResponse(String pairCode, boolean finalRung) {
        this.pairCode = pairCode;
        this.finalRung = finalRung;
    }

    public String getPairCode() {
        return pairCode;
    }

    public boolean isFinalRung() {
        return finalRung;
    }
}
