package io.github.nilsfjp.ideophonearena.dto;

public class DivergenceResponse {

    private Long ideophoneId;
    private String romaji;
    private String gloss;
    private String modality;
    private Double guessAccuracy;
    private long guessCount;
    private Double meanRating;
    private long ratingCount;

    public DivergenceResponse(Long ideophoneId, String romaji, String gloss, String modality,
            Double guessAccuracy, long guessCount, Double meanRating, long ratingCount) {
        this.ideophoneId = ideophoneId;
        this.romaji = romaji;
        this.gloss = gloss;
        this.modality = modality;
        this.guessAccuracy = guessAccuracy;
        this.guessCount = guessCount;
        this.meanRating = meanRating;
        this.ratingCount = ratingCount;
    }

    public Long getIdeophoneId() {
        return ideophoneId;
    }

    public String getRomaji() {
        return romaji;
    }

    public String getGloss() {
        return gloss;
    }

    public String getModality() {
        return modality;
    }

    public Double getGuessAccuracy() {
        return guessAccuracy;
    }

    public long getGuessCount() {
        return guessCount;
    }

    public Double getMeanRating() {
        return meanRating;
    }

    public long getRatingCount() {
        return ratingCount;
    }
}
