package io.github.nilsfjp.ideophonearena.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ProductionRequest {

    @NotNull
    @Positive
    private Long ideophoneId;

    // The romaji gate lives in PhonologyService, not in a @Pattern here: the input is
    // trimmed and lowercased before it is gated, and an unparseable form must surface the
    // same validationErrors.input shape a Bean Validation failure would.
    @NotBlank
    private String input;

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

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
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
