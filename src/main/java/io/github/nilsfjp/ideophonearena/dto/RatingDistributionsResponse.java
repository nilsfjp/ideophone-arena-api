package io.github.nilsfjp.ideophonearena.dto;

import java.util.List;
import java.util.Map;

// Population distribution of 1-7 iconicity ratings per modality, for the
// Observatory raincloud panels. `distributions` is a dense 1-7 grid for every
// modality that has at least one rating (modalities with none are omitted);
// `byModalityN` gives each present modality's total rating count so specimen
// labels stay honest.
public class RatingDistributionsResponse {

    private List<RatingDistributionCell> distributions;
    private Map<String, Long> byModalityN;

    public RatingDistributionsResponse(List<RatingDistributionCell> distributions, Map<String, Long> byModalityN) {
        this.distributions = distributions;
        this.byModalityN = byModalityN;
    }

    public List<RatingDistributionCell> getDistributions() {
        return distributions;
    }

    public Map<String, Long> getByModalityN() {
        return byModalityN;
    }
}
