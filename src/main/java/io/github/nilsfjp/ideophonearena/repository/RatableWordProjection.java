package io.github.nilsfjp.ideophonearena.repository;

import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import java.time.Instant;

public interface RatableWordProjection {

    Long getIdeophoneId();

    String getCanonicalForm();

    String getRomaji();

    String getStimulusFile();

    Modality getModality();

    String getGloss();

    Instant getFirstAnsweredAt();
}
