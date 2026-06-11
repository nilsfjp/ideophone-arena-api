package io.github.nilsfjp.ideophonearena.repository;

import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;

public interface ConditionAnswerStatsProjection {

    ConditionName getConditionName();

    Long getAnswers();

    Long getCorrect();
}
