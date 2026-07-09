package io.github.nilsfjp.ideophonearena.dto;

import java.time.Instant;

public class ProductionEntryResponse {

    private Long id;
    private Long ideophoneId;
    private String input;
    private int similarityScore;
    private Instant createdAt;

    public ProductionEntryResponse(Long id, Long ideophoneId, String input, int similarityScore,
            Instant createdAt) {
        this.id = id;
        this.ideophoneId = ideophoneId;
        this.input = input;
        this.similarityScore = similarityScore;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getIdeophoneId() {
        return ideophoneId;
    }

    public String getInput() {
        return input;
    }

    public int getSimilarityScore() {
        return similarityScore;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
