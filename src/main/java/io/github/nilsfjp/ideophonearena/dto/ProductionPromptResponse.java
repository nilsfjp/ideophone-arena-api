package io.github.nilsfjp.ideophonearena.dto;

// A meaning-only prompt, or the completed sentinel once the caller has produced every
// word in the cycle (the RoundResponse completion precedent). No kana, no audio, no
// romaji: the form is exactly what the player has to invent.
public class ProductionPromptResponse {

    private boolean completed;
    private Long ideophoneId;
    private String gloss;
    private String modality;
    // Size of the producible universe -- the {n} in Word Mint's "word {i} of {n}". Caller-
    // invariant, and carried on the completed sentinel too so the status line survives the
    // last round. The client must never derive this count itself.
    private long totalProducible;

    public ProductionPromptResponse(boolean completed, Long ideophoneId, String gloss, String modality,
            long totalProducible) {
        this.completed = completed;
        this.ideophoneId = ideophoneId;
        this.gloss = gloss;
        this.modality = modality;
        this.totalProducible = totalProducible;
    }

    public boolean isCompleted() {
        return completed;
    }

    public Long getIdeophoneId() {
        return ideophoneId;
    }

    public String getGloss() {
        return gloss;
    }

    public String getModality() {
        return modality;
    }

    public long getTotalProducible() {
        return totalProducible;
    }
}
