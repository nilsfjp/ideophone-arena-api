package io.github.nilsfjp.ideophonearena.dto;

import io.github.nilsfjp.ideophonearena.model.enums.Modality;

public class IdeophoneChoiceResponse {

    private Long ideophoneId;
    private String kana;
    private String romaji;
    private String stimulusFile;
    private String stimulusUrl;
    private Modality modality;
    private String canonicalScript;

    public IdeophoneChoiceResponse(Long ideophoneId, String kana, String romaji, String stimulusFile,
            String stimulusUrl, Modality modality, String canonicalScript) {
        this.ideophoneId = ideophoneId;
        this.kana = kana;
        this.romaji = romaji;
        this.stimulusFile = stimulusFile;
        this.stimulusUrl = stimulusUrl;
        this.modality = modality;
        this.canonicalScript = canonicalScript;
    }

    public Long getIdeophoneId() {
        return ideophoneId;
    }

    public String getKana() {
        return kana;
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

    public String getCanonicalScript() {
        return canonicalScript;
    }
}
