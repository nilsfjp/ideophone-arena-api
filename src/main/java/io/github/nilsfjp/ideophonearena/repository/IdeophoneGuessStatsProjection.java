package io.github.nilsfjp.ideophonearena.repository;

public interface IdeophoneGuessStatsProjection {

    Long getIdeophoneId();

    Long getGuesses();

    Long getCorrect();
}
