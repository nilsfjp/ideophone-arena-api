package io.github.nilsfjp.ideophonearena.dto;

import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;
import io.github.nilsfjp.ideophonearena.model.enums.GameMode;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import java.time.Instant;

public class GameSessionResponse {

    private String sessionUuid;
    private ConditionName conditionName;
    private GameMode gameMode;
    private Modality floor;
    private boolean includePractice;
    private Instant startedAt;
    private int totalRounds;

    public GameSessionResponse(String sessionUuid, ConditionName conditionName, GameMode gameMode,
            Modality floor, boolean includePractice, Instant startedAt, int totalRounds) {
        this.sessionUuid = sessionUuid;
        this.conditionName = conditionName;
        this.gameMode = gameMode;
        this.floor = floor;
        this.includePractice = includePractice;
        this.startedAt = startedAt;
        this.totalRounds = totalRounds;
    }

    public String getSessionUuid() {
        return sessionUuid;
    }

    public ConditionName getConditionName() {
        return conditionName;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public Modality getFloor() {
        return floor;
    }

    public boolean isIncludePractice() {
        return includePractice;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    // Scored rounds this session will serve, so the client never has to guess the
    // denominator of "Round n / total". Practice rounds are excluded: they are not
    // scored and the progress display counts scored rounds only.
    public int getTotalRounds() {
        return totalRounds;
    }
}
