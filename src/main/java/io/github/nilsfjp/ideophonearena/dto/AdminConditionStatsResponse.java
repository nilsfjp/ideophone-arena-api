package io.github.nilsfjp.ideophonearena.dto;

public class AdminConditionStatsResponse {

    private String conditionName;
    private long sessions;
    private long answers;
    private long correct;
    private double accuracy;

    public AdminConditionStatsResponse(String conditionName, long sessions, long answers, long correct,
            double accuracy) {
        this.conditionName = conditionName;
        this.sessions = sessions;
        this.answers = answers;
        this.correct = correct;
        this.accuracy = accuracy;
    }

    public String getConditionName() {
        return conditionName;
    }

    public long getSessions() {
        return sessions;
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
