package io.github.nilsfjp.ideophonearena.dto;

import java.time.Instant;

public class AttemptResponse {

    private Instant answeredAt;
    private String targetTranslation;
    private String selectedKana;
    private String correctKana;
    private boolean correct;
    private Integer responseTimeMs;

    public AttemptResponse(Instant answeredAt, String targetTranslation, String selectedKana, String correctKana,
            boolean correct, Integer responseTimeMs) {
        this.answeredAt = answeredAt;
        this.targetTranslation = targetTranslation;
        this.selectedKana = selectedKana;
        this.correctKana = correctKana;
        this.correct = correct;
        this.responseTimeMs = responseTimeMs;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }

    public String getTargetTranslation() {
        return targetTranslation;
    }

    public String getPrompt() {
        return targetTranslation;
    }

    public String getSelectedKana() {
        return selectedKana;
    }

    public String getCorrectKana() {
        return correctKana;
    }

    public boolean isCorrect() {
        return correct;
    }

    public Integer getResponseTimeMs() {
        return responseTimeMs;
    }
}
