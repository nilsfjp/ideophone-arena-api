package io.github.nilsfjp.ideophonearena.repository;

import io.github.nilsfjp.ideophonearena.model.enums.Modality;

// One completed Perception Ladder session's score, keyed by its floor. LadderService
// reduces these to the caller's best session per floor for the ladder overview.
public interface LadderFloorScoreProjection {

    Modality getFloor();

    Long getAnswered();

    Long getCorrect();
}
