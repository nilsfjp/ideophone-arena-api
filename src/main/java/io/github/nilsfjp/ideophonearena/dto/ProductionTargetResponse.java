package io.github.nilsfjp.ideophonearena.dto;

// The attested word, revealed after submit. displayForm is words.canonical_form verbatim
// (ADR-0: production is not a scripted-condition surface, so there is no presentation
// lookup). The player's own input is never converted to kana -- invariant 1.
public class ProductionTargetResponse {

    private String displayForm;
    private String romaji;
    private String gloss;
    private String stimulusUrl;

    public ProductionTargetResponse(String displayForm, String romaji, String gloss, String stimulusUrl) {
        this.displayForm = displayForm;
        this.romaji = romaji;
        this.gloss = gloss;
        this.stimulusUrl = stimulusUrl;
    }

    public String getDisplayForm() {
        return displayForm;
    }

    public String getRomaji() {
        return romaji;
    }

    public String getGloss() {
        return gloss;
    }

    public String getStimulusUrl() {
        return stimulusUrl;
    }
}
