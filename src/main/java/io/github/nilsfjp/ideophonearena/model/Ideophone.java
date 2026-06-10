package io.github.nilsfjp.ideophonearena.model;

import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "ideophones",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"kana", "canonical_script"})
        }
)
public class Ideophone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String kana;

    @Column(name = "display_form", nullable = false, length = 50)
    private String displayForm;

    @Column(name = "canonical_form", nullable = false, length = 50)
    private String canonicalForm;

    @Column(nullable = false, length = 100)
    private String romaji;

    @Column(nullable = false)
    private String gloss;

    @Column(name = "canonical_script", nullable = false, length = 20)
    private String canonicalScript;

    @Column(name = "stimulus_file", nullable = false, length = 100)
    private String stimulusFile;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Modality modality;

    protected Ideophone() {
    }

    public Ideophone(String kana, String displayForm, String canonicalForm, String romaji, String gloss,
            String canonicalScript, String stimulusFile, Modality modality) {
        this.kana = kana;
        this.displayForm = displayForm;
        this.canonicalForm = canonicalForm;
        this.romaji = romaji;
        this.gloss = gloss;
        this.canonicalScript = canonicalScript;
        this.stimulusFile = stimulusFile;
        this.modality = modality;
    }

    public Long getId() {
        return id;
    }

    public String getKana() {
        return kana;
    }

    public void setKana(String kana) {
        this.kana = kana;
    }

    public String getDisplayForm() {
        return displayForm;
    }

    public void setDisplayForm(String displayForm) {
        this.displayForm = displayForm;
    }

    public String getCanonicalForm() {
        return canonicalForm;
    }

    public void setCanonicalForm(String canonicalForm) {
        this.canonicalForm = canonicalForm;
    }

    public String getRomaji() {
        return romaji;
    }

    public void setRomaji(String romaji) {
        this.romaji = romaji;
    }

    public String getGloss() {
        return gloss;
    }

    public void setGloss(String gloss) {
        this.gloss = gloss;
    }

    public String getCanonicalScript() {
        return canonicalScript;
    }

    public void setCanonicalScript(String canonicalScript) {
        this.canonicalScript = canonicalScript;
    }

    public String getStimulusFile() {
        return stimulusFile;
    }

    public void setStimulusFile(String stimulusFile) {
        this.stimulusFile = stimulusFile;
    }

    public Modality getModality() {
        return modality;
    }

    public void setModality(Modality modality) {
        this.modality = modality;
    }
}
