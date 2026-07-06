package io.github.nilsfjp.ideophonearena.dto;

// One raincloud cell: the count of a single 1-7 rating value within one
// modality. Per-value counts (not means) so the client can draw the
// distribution; zero-count cells are emitted for every modality that has any
// ratings, so the 1-7 axis is complete.
public class RatingDistributionCell {

    private String modality;
    private int ratingValue;
    private long count;

    public RatingDistributionCell(String modality, int ratingValue, long count) {
        this.modality = modality;
        this.ratingValue = ratingValue;
        this.count = count;
    }

    public String getModality() {
        return modality;
    }

    public int getRatingValue() {
        return ratingValue;
    }

    public long getCount() {
        return count;
    }
}
