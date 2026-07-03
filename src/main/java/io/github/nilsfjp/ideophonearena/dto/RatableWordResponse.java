package io.github.nilsfjp.ideophonearena.dto;

import io.github.nilsfjp.ideophonearena.model.enums.Modality;

// One word the caller may rate in the Rating Lab: encountered through an
// answered (scored) round, not yet rated by this user. The meaning is the
// word's own gloss -- exactly the mapping the round's feedback revealed, so
// rating never exposes a meaning before naive guessing.
public class RatableWordResponse {

    private Long ideophoneId;
    private String canonicalForm;
    private String romaji;
    private String stimulusFile;
    private Modality modality;
    private String meaning;

    public RatableWordResponse(Long ideophoneId, String canonicalForm, String romaji, String stimulusFile,
            Modality modality, String meaning) {
        this.ideophoneId = ideophoneId;
        this.canonicalForm = canonicalForm;
        this.romaji = romaji;
        this.stimulusFile = stimulusFile;
        this.modality = modality;
        this.meaning = meaning;
    }

    public Long getIdeophoneId() {
        return ideophoneId;
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

    public Modality getModality() {
        return modality;
    }

    public String getMeaning() {
        return meaning;
    }
}
