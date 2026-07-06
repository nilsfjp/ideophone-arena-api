package io.github.nilsfjp.ideophonearena.repository;

import io.github.nilsfjp.ideophonearena.model.enums.Modality;

// One (modality, ratingValue) cell of the population rating distribution: how
// many 1-7 ratings of that value were given to words of that modality.
public interface ModalityRatingDistributionProjection {

    Modality getModality();

    Short getRatingValue();

    Long getCount();
}
