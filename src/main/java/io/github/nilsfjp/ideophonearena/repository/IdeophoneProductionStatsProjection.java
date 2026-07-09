package io.github.nilsfjp.ideophonearena.repository;

// Production side of the triangulation aggregate: one row per word.
public interface IdeophoneProductionStatsProjection {

    Long getIdeophoneId();

    Long getProductionCount();

    Double getMeanProductionScore();
}
