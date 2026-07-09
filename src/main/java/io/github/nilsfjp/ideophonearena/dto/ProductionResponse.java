package io.github.nilsfjp.ideophonearena.dto;

import java.util.List;

public class ProductionResponse {

    private Long id;
    private Long ideophoneId;
    private String input;
    private int similarityScore;
    private List<FeatureMatchResponse> features;
    private ProductionTargetResponse target;

    public ProductionResponse(Long id, Long ideophoneId, String input, int similarityScore,
            List<FeatureMatchResponse> features, ProductionTargetResponse target) {
        this.id = id;
        this.ideophoneId = ideophoneId;
        this.input = input;
        this.similarityScore = similarityScore;
        this.features = features;
        this.target = target;
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

    public List<FeatureMatchResponse> getFeatures() {
        return features;
    }

    public ProductionTargetResponse getTarget() {
        return target;
    }
}
