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
import java.math.BigDecimal;
import java.time.LocalDate;

// The durable pair-level inventory (ADR-6): the thesis's unit of difficulty.
// Members are FKs to words; is_core marks invariant-4 content and thesis_accuracy
// carries the exact per-pair prior where the thesis measured it.
@Entity
@Table(name = "pairings")
public class Pairing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pair_code", nullable = false, unique = true, length = 20)
    private String pairCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_a_id", nullable = false)
    private Word wordA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_b_id", nullable = false)
    private Word wordB;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Modality modality;

    @Column(name = "is_core", nullable = false)
    private boolean core;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(name = "difficulty_prior", length = 10)
    private String difficultyPrior;

    @Column(name = "thesis_accuracy", precision = 5, scale = 4)
    private BigDecimal thesisAccuracy;

    @Column(name = "foil_distance", precision = 6, scale = 4)
    private BigDecimal foilDistance;

    @Column(name = "signoff_ref", length = 100)
    private String signoffRef;

    @Column(name = "approved_at")
    private LocalDate approvedAt;

    protected Pairing() {
    }

    public Pairing(String pairCode, Language language, Word wordA, Word wordB, Modality modality, boolean core,
            String source) {
        this.pairCode = pairCode;
        this.language = language;
        this.wordA = wordA;
        this.wordB = wordB;
        this.modality = modality;
        this.core = core;
        this.source = source;
    }

    public Long getId() {
        return id;
    }

    public String getPairCode() {
        return pairCode;
    }

    public Language getLanguage() {
        return language;
    }

    public Word getWordA() {
        return wordA;
    }

    public Word getWordB() {
        return wordB;
    }

    public Modality getModality() {
        return modality;
    }

    public boolean isCore() {
        return core;
    }

    public String getSource() {
        return source;
    }

    public String getDifficultyPrior() {
        return difficultyPrior;
    }

    public BigDecimal getThesisAccuracy() {
        return thesisAccuracy;
    }

    public void setThesisAccuracy(BigDecimal thesisAccuracy) {
        this.thesisAccuracy = thesisAccuracy;
    }

    public BigDecimal getFoilDistance() {
        return foilDistance;
    }

    public String getSignoffRef() {
        return signoffRef;
    }

    public LocalDate getApprovedAt() {
        return approvedAt;
    }
}
