package io.github.nilsfjp.ideophonearena.dto;

import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;
import jakarta.validation.constraints.Positive;

public class StartSessionRequest {

    @Positive
    private Integer difficultyLevel;

    private ConditionName conditionName;

    public Integer getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(Integer difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public ConditionName getConditionName() {
        return conditionName;
    }

    public void setConditionName(ConditionName conditionName) {
        this.conditionName = conditionName;
    }
}
