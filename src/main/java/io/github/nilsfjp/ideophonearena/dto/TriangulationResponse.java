package io.github.nilsfjp.ideophonearena.dto;

// One word, three measures (ADR-5 word grain): how guessable its meaning is, how iconic
// players rate it, and how closely naive players reinvent its form. Each measure is null
// when its count is zero, so "no data" stays distinct from "always wrong" / "lowest score".
public class TriangulationResponse {

    private Long ideophoneId;
    private String romaji;
    private String gloss;
    private String modality;
    private Double guessAccuracy;
    private long guessCount;
    private Double meanRating;
    private long ratingCount;
    private Double meanProductionScore;
    private long productionCount;

    public TriangulationResponse(Long ideophoneId, String romaji, String gloss, String modality,
            Double guessAccuracy, long guessCount, Double meanRating, long ratingCount,
            Double meanProductionScore, long productionCount) {
        this.ideophoneId = ideophoneId;
        this.romaji = romaji;
        this.gloss = gloss;
        this.modality = modality;
        this.guessAccuracy = guessAccuracy;
        this.guessCount = guessCount;
        this.meanRating = meanRating;
        this.ratingCount = ratingCount;
        this.meanProductionScore = meanProductionScore;
        this.productionCount = productionCount;
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

    public Double getMeanProductionScore() {
        return meanProductionScore;
    }

    public long getProductionCount() {
        return productionCount;
    }
}
