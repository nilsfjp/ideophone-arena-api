package io.github.nilsfjp.ideophonearena.dto;

import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;
import java.time.Instant;

public class GameSessionResponse {

    private String sessionUuid;
    private int difficultyLevel;
    private ConditionName conditionName;
    private Instant startedAt;

    public GameSessionResponse(String sessionUuid, int difficultyLevel, ConditionName conditionName,
            Instant startedAt) {
        this.sessionUuid = sessionUuid;
        this.difficultyLevel = difficultyLevel;
        this.conditionName = conditionName;
        this.startedAt = startedAt;
    }

    public String getSessionUuid() {
        return sessionUuid;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public ConditionName getConditionName() {
        return conditionName;
    }

    public Instant getStartedAt() {
        return startedAt;
    }
}
