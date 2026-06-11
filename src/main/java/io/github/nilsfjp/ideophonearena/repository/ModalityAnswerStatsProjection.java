package io.github.nilsfjp.ideophonearena.repository;

import io.github.nilsfjp.ideophonearena.model.enums.Modality;

public interface ModalityAnswerStatsProjection {

    Modality getModality();

    Long getAnswers();

    Long getCorrect();
}
