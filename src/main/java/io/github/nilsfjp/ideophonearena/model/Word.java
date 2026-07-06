package io.github.nilsfjp.ideophonearena.model;

import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// Word identity as an entity (ADR-0). One row per word; presentation-invariant
// facts (canonical form, gloss, modality, and the single shared audio file --
// invariant 2 now structural) live here once, not duplicated per script condition.
@Entity
@Table(
        name = "words",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"language_id", "romaji"})
        }
)
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @Column(nullable = false, length = 100)
    private String romaji;

    @Column(nullable = false, length = 50)
    private String kana;

    @Column(name = "canonical_form", nullable = false, length = 50)
    private String canonicalForm;

    // The word-level script: 'H' | 'K' (first letter of the legacy 2-letter code).
    @Column(name = "canonical_script", nullable = false, length = 20)
    private String canonicalScript;

    @Column(nullable = false)
    private String gloss;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Modality modality;

    // The dataset's own taxonomy (Sound/Motion/...); never unified with modality.
    @Column(name = "semantic_category", length = 20)
    private String semanticCategory;

    // Invariant 2 as structure: one audio per word, by schema.
    @Column(name = "stimulus_file", nullable = false, length = 100)
    private String stimulusFile;

    protected Word() {
    }

    public Word(Language language, String romaji, String kana, String canonicalForm, String canonicalScript,
            String gloss, Modality modality, String stimulusFile) {
        this.language = language;
        this.romaji = romaji;
        this.kana = kana;
        this.canonicalForm = canonicalForm;
        this.canonicalScript = canonicalScript;
        this.gloss = gloss;
        this.modality = modality;
        this.stimulusFile = stimulusFile;
    }

    public Long getId() {
        return id;
    }

    public Language getLanguage() {
        return language;
    }

    public String getRomaji() {
        return romaji;
    }

    public String getKana() {
        return kana;
    }

    public String getCanonicalForm() {
        return canonicalForm;
    }

    public String getCanonicalScript() {
        return canonicalScript;
    }

    public String getGloss() {
        return gloss;
    }

    public Modality getModality() {
        return modality;
    }

    public void setModality(Modality modality) {
        this.modality = modality;
    }

    public String getSemanticCategory() {
        return semanticCategory;
    }

    public String getStimulusFile() {
        return stimulusFile;
    }
}
