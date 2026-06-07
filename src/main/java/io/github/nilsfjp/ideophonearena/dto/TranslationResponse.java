package io.github.nilsfjp.ideophonearena.dto;

public class TranslationResponse {

    private String target;
    private String other;

    public TranslationResponse(String target, String other) {
        this.target = target;
        this.other = other;
    }

    public String getTarget() {
        return target;
    }

    public String getOther() {
        return other;
    }
}
