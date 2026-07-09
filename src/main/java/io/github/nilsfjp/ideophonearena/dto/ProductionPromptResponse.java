package io.github.nilsfjp.ideophonearena.dto;

// A meaning-only prompt, or the completed sentinel once the caller has produced every
// word in the cycle (the RoundResponse completion precedent). No kana, no audio, no
// romaji: the form is exactly what the player has to invent.
public class ProductionPromptResponse {

    private boolean completed;
    private Long ideophoneId;
    private String gloss;
    private String modality;

    public ProductionPromptResponse(boolean completed, Long ideophoneId, String gloss, String modality) {
        this.completed = completed;
        this.ideophoneId = ideophoneId;
        this.gloss = gloss;
        this.modality = modality;
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
}
