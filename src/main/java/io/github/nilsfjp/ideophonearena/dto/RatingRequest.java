package io.github.nilsfjp.ideophonearena.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class RatingRequest {

    @NotNull
    @Positive
    private Long ideophoneId;

    @NotNull
    @Min(1)
    @Max(7)
    private Integer rating;

    @Min(0)
    @Max(600000)
    private Integer responseTimeMs;

    private String sessionUuid;

    public Long getIdeophoneId() {
        return ideophoneId;
    }

    public void setIdeophoneId(Long ideophoneId) {
        this.ideophoneId = ideophoneId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Integer getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Integer responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public String getSessionUuid() {
        return sessionUuid;
    }

    public void setSessionUuid(String sessionUuid) {
        this.sessionUuid = sessionUuid;
    }
}
