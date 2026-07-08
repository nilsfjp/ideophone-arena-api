package io.github.nilsfjp.ideophonearena.dto;

import io.github.nilsfjp.ideophonearena.model.enums.Modality;

public class IdeophoneChoiceResponse {

    private Long ideophoneId;
    private String kana;
    private String displayForm;
    private String canonicalForm;
    private String romaji;
    private String stimulusFile;
    private String stimulusUrl;
    private Modality modality;

    public IdeophoneChoiceResponse(Long ideophoneId, String kana, String displayForm, String canonicalForm,
            String romaji, String stimulusFile, String stimulusUrl, Modality modality) {
        this.ideophoneId = ideophoneId;
        this.kana = kana;
        this.displayForm = displayForm;
        this.canonicalForm = canonicalForm;
        this.romaji = romaji;
        this.stimulusFile = stimulusFile;
        this.stimulusUrl = stimulusUrl;
        this.modality = modality;
    }

    public Long getIdeophoneId() {
        return ideophoneId;
    }

    public String getKana() {
        return kana;
    }

    public String getDisplayForm() {
        return displayForm;
    }

    public String getCanonicalForm() {
        return canonicalForm;
    }

    public String getRomaji() {
        return romaji;
    }

    public String getStimulusFile() {
        return stimulusFile;
    }

    public String getStimulusUrl() {
        return stimulusUrl;
    }

    public Modality getModality() {
        return modality;
    }
}
