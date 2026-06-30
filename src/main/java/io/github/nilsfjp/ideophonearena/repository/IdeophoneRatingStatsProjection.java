package io.github.nilsfjp.ideophonearena.repository;

public interface IdeophoneRatingStatsProjection {

    Long getIdeophoneId();

    Long getRatingCount();

    Double getMeanRating();
}
