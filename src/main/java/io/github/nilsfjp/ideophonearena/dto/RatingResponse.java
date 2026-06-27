package io.github.nilsfjp.ideophonearena.dto;

import java.time.Instant;

public class RatingResponse {

    private Long id;
    private Long ideophoneId;
    private int rating;
    private Integer responseTimeMs;
    private Instant ratedAt;

    public RatingResponse(Long id, Long ideophoneId, int rating, Integer responseTimeMs, Instant ratedAt) {
        this.id = id;
        this.ideophoneId = ideophoneId;
        this.rating = rating;
        this.responseTimeMs = responseTimeMs;
        this.ratedAt = ratedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getIdeophoneId() {
        return ideophoneId;
    }

    public int getRating() {
        return rating;
    }

    public Integer getResponseTimeMs() {
        return responseTimeMs;
    }

    public Instant getRatedAt() {
        return ratedAt;
    }
}
