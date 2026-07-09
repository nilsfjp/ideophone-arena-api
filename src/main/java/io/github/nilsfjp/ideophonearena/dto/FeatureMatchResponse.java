package io.github.nilsfjp.ideophonearena.dto;

// One feature chip on the reveal card. yours/target are heterogeneous by design: five of
// the seven features are booleans, moraCount is an integer, heavyVowelRatio a decimal.
// The client knows each feature's kind from its key (SPEC-view-designs section 8.3).
// matched is true when the feature contributed its full weight to the score.
public class FeatureMatchResponse {

    private String feature;
    private Object yours;
    private Object target;
    private boolean matched;

    public FeatureMatchResponse(String feature, Object yours, Object target, boolean matched) {
        this.feature = feature;
        this.yours = yours;
        this.target = target;
        this.matched = matched;
    }

    public String getFeature() {
        return feature;
    }

    public Object getYours() {
        return yours;
    }

    public Object getTarget() {
        return target;
    }

    public boolean isMatched() {
        return matched;
    }
}
