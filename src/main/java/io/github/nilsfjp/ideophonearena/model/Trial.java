package io.github.nilsfjp.ideophonearena.model;

import io.github.nilsfjp.ideophonearena.model.enums.RoundType;
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

// One servable trial (ADR-3): the condition dimension is collapsed away, so a
// pairing serves a single trial (34 total). Membership lives on the pairing;
// correct_word_id documents the thesis fixed target (unread in the CHOOSING
// serving path, which derives its target from the shuffle).
@Entity
@Table(name = "trials")
public class Trial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "round_type", nullable = false, length = 20)
    private RoundType roundType = RoundType.CHOOSING;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pairing_id", nullable = false)
    private Pairing pairing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "correct_word_id")
    private Word correctWord;

    @Column(name = "feature_axis", length = 10)
    private String featureAxis;

    @Column(name = "is_practice", nullable = false)
    private boolean practice;

    protected Trial() {
    }

    public Trial(RoundType roundType, Pairing pairing, Word correctWord, boolean practice) {
        this.roundType = roundType;
        this.pairing = pairing;
        this.correctWord = correctWord;
        this.practice = practice;
    }

    public Trial(Pairing pairing, Word correctWord, boolean practice) {
        this(RoundType.CHOOSING, pairing, correctWord, practice);
    }

    public Long getId() {
        return id;
    }

    public RoundType getRoundType() {
        return roundType;
    }

    public Pairing getPairing() {
        return pairing;
    }

    public Word getCorrectWord() {
        return correctWord;
    }

    public String getFeatureAxis() {
        return featureAxis;
    }

    public boolean isPractice() {
        return practice;
    }
}
