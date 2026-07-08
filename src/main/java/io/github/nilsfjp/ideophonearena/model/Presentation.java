package io.github.nilsfjp.ideophonearena.model;

import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;
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

// The script manipulation, and nothing else (ADR-0): one row per word x scripted
// condition. display_form is the exact kana string the player sees (invariant 1);
// script_code is the legacy 2-letter provenance code (no longer exposed to the
// frontend as of A7/NIL-41; the column stays).
@Entity
@Table(
        name = "presentations",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"word_id", "condition_name"})
        }
)
public class Presentation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_id", nullable = false)
    private Word word;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_name", nullable = false, length = 50)
    private ConditionName conditionName;

    @Column(name = "display_form", nullable = false, length = 50)
    private String displayForm;

    @Column(name = "script_code", nullable = false, length = 2)
    private String scriptCode;

    protected Presentation() {
    }

    public Presentation(Word word, ConditionName conditionName, String displayForm, String scriptCode) {
        this.word = word;
        this.conditionName = conditionName;
        this.displayForm = displayForm;
        this.scriptCode = scriptCode;
    }

    public Long getId() {
        return id;
    }

    public Word getWord() {
        return word;
    }

    public ConditionName getConditionName() {
        return conditionName;
    }

    public String getDisplayForm() {
        return displayForm;
    }

    public String getScriptCode() {
        return scriptCode;
    }
}
