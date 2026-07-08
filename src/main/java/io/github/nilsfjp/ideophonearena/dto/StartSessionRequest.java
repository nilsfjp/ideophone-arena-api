package io.github.nilsfjp.ideophonearena.dto;

import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;
import io.github.nilsfjp.ideophonearena.model.enums.GameMode;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import jakarta.validation.constraints.NotNull;

public class StartSessionRequest {

    @NotNull
    private ConditionName conditionName;

    private boolean includePractice;

    // M1: optional play mode; absent means CHOOSING (the core Meaning Match loop).
    private GameMode gameMode;

    // Perception Ladder floor to serve (a Modality). Required iff gameMode == LADDER and
    // forbidden otherwise -- validated in GameService. The ladder never overloads
    // difficultyLevel (A3): floor selection is this explicit parameter.
    private Modality floor;

    public ConditionName getConditionName() {
        return conditionName;
    }

    public void setConditionName(ConditionName conditionName) {
        this.conditionName = conditionName;
    }

    public boolean isIncludePractice() {
        return includePractice;
    }

    public void setIncludePractice(boolean includePractice) {
        this.includePractice = includePractice;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public Modality getFloor() {
        return floor;
    }

    public void setFloor(Modality floor) {
        this.floor = floor;
    }
}
