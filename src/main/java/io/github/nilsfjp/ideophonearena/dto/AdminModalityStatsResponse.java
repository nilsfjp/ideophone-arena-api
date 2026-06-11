package io.github.nilsfjp.ideophonearena.dto;

public class AdminModalityStatsResponse {

    private String modality;
    private long answers;
    private long correct;
    private double accuracy;

    public AdminModalityStatsResponse(String modality, long answers, long correct, double accuracy) {
        this.modality = modality;
        this.answers = answers;
        this.correct = correct;
        this.accuracy = accuracy;
    }

    public String getModality() {
        return modality;
    }

    public long getAnswers() {
        return answers;
    }

    public long getCorrect() {
        return correct;
    }

    public double getAccuracy() {
        return accuracy;
    }
}
