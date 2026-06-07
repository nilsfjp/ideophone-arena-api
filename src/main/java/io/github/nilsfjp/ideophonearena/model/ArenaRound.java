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

@Entity
@Table(name = "arena_rounds")
public class ArenaRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String prompt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "left_ideophone_id", nullable = false)
    private Ideophone leftIdeophone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "right_ideophone_id", nullable = false)
    private Ideophone rightIdeophone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "correct_ideophone_id", nullable = false)
    private Ideophone correctIdeophone;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_name", nullable = false, length = 50)
    private ConditionName conditionName = ConditionName.TEXT_ONLY;

    @Column(name = "difficulty_level", nullable = false)
    private int difficultyLevel = 1;

    protected ArenaRound() {
    }

    public ArenaRound(String prompt, Ideophone leftIdeophone, Ideophone rightIdeophone, Ideophone correctIdeophone,
            ConditionName conditionName, int difficultyLevel) {
        this.prompt = prompt;
        this.leftIdeophone = leftIdeophone;
        this.rightIdeophone = rightIdeophone;
        this.correctIdeophone = correctIdeophone;
        this.conditionName = conditionName;
        this.difficultyLevel = difficultyLevel;
    }

    public Long getId() {
        return id;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Ideophone getLeftIdeophone() {
        return leftIdeophone;
    }

    public void setLeftIdeophone(Ideophone leftIdeophone) {
        this.leftIdeophone = leftIdeophone;
    }

    public Ideophone getRightIdeophone() {
        return rightIdeophone;
    }

    public void setRightIdeophone(Ideophone rightIdeophone) {
        this.rightIdeophone = rightIdeophone;
    }

    public Ideophone getCorrectIdeophone() {
        return correctIdeophone;
    }

    public void setCorrectIdeophone(Ideophone correctIdeophone) {
        this.correctIdeophone = correctIdeophone;
    }

    public ConditionName getConditionName() {
        return conditionName;
    }

    public void setConditionName(ConditionName conditionName) {
        this.conditionName = conditionName;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }
}
