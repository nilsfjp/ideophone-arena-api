package io.github.nilsfjp.ideophonearena.repository;

import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;

public interface ConditionSessionCountProjection {

    ConditionName getConditionName();

    Long getSessions();
}
