package io.github.nilsfjp.ideophonearena.repository;

public interface LeaderboardEntryProjection {

    Long getUserId();

    String getUsername();

    Long getTotalAnswers();

    Long getCorrectAnswers();

    Double getAverageResponseTimeMs();
}
