package io.github.nilsfjp.ideophonearena.dto;

public class TimingResponse {

    private int fixationMs;
    private int preChoiceDelayMs;

    public TimingResponse(int fixationMs, int preChoiceDelayMs) {
        this.fixationMs = fixationMs;
        this.preChoiceDelayMs = preChoiceDelayMs;
    }

    public int getFixationMs() {
        return fixationMs;
    }

    public int getPreChoiceDelayMs() {
        return preChoiceDelayMs;
    }
}
