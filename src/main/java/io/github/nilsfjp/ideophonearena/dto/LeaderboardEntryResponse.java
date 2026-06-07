package io.github.nilsfjp.ideophonearena.dto;

public class LeaderboardEntryResponse {

    private String username;
    private long totalAnswered;
    private long totalCorrect;
    private double accuracy;

    public LeaderboardEntryResponse(String username, long totalAnswered, long totalCorrect, double accuracy) {
        this.username = username;
        this.totalAnswered = totalAnswered;
        this.totalCorrect = totalCorrect;
        this.accuracy = accuracy;
    }

    public String getUsername() {
        return username;
    }

    public long getTotalAnswered() {
        return totalAnswered;
    }

    public long getTotalCorrect() {
        return totalCorrect;
    }

    public double getAccuracy() {
        return accuracy;
    }
}
