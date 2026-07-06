package io.github.nilsfjp.ideophonearena.mapper;

import io.github.nilsfjp.ideophonearena.dto.DivergenceResponse;
import io.github.nilsfjp.ideophonearena.dto.PositionBiasResponse;
import io.github.nilsfjp.ideophonearena.dto.RatingDistributionCell;
import io.github.nilsfjp.ideophonearena.dto.RatingDistributionsResponse;
import io.github.nilsfjp.ideophonearena.model.Ideophone;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ResearchMapper {

    public DivergenceResponse toDivergenceResponse(Ideophone ideophone, long guessCount, long correct,
            long ratingCount, Double meanRating) {
        // No guesses or no ratings encodes as null (not 0.0) so the client can
        // tell "no data" apart from "always wrong" / "lowest rating".
        Double guessAccuracy = guessCount == 0 ? null : (double) correct / guessCount;
        String modality = ideophone.getModality() == null ? null : ideophone.getModality().name();
        return new DivergenceResponse(
                ideophone.getId(),
                ideophone.getRomaji(),
                ideophone.getDisplayForm(),
                ideophone.getGloss(),
                modality,
                guessAccuracy,
                guessCount,
                meanRating,
                ratingCount);
    }

    // Lays the per-modality bins out as a dense 1-7 grid, ordered by Modality
    // enum ordinal then rating value; each bin array is indexed 0..6 for rating
    // values 1..7. Modalities with no ratings (a null bin array) are omitted
    // entirely, and byModalityN keeps their totals so specimen labels stay
    // honest.
    public RatingDistributionsResponse toRatingDistributions(Map<Modality, long[]> countsByModality,
            Map<Modality, Long> nByModality) {
        List<RatingDistributionCell> distributions = new ArrayList<>();
        Map<String, Long> byModalityN = new LinkedHashMap<>();
        for (Modality modality : Modality.values()) {
            long[] bins = countsByModality.get(modality);
            if (bins == null) {
                continue;
            }
            for (int index = 0; index < bins.length; index++) {
                distributions.add(new RatingDistributionCell(modality.name(), index + 1, bins[index]));
            }
            byModalityN.put(modality.name(), nByModality.getOrDefault(modality, 0L));
        }
        return new RatingDistributionsResponse(distributions, byModalityN);
    }

    // Assembles the position-bias DTO from the service's tallies. Rates and
    // accuracies are null (not 0.0) when their denominator is 0, mirroring the
    // divergence null-vs-zero convention; d'/criterion arrive already null when
    // a stimulus class was empty.
    public PositionBiasResponse toPositionBiasResponse(long n, long leftPickCount, Double dPrime, Double criterion,
            long targetTopN, long targetTopCorrect, long targetBottomN, long targetBottomCorrect) {
        long rightPickCount = n - leftPickCount;
        Double leftPickRate = n == 0 ? null : (double) leftPickCount / n;
        Double targetTopAccuracy = targetTopN == 0 ? null : (double) targetTopCorrect / targetTopN;
        Double targetBottomAccuracy = targetBottomN == 0 ? null : (double) targetBottomCorrect / targetBottomN;
        return new PositionBiasResponse(n, leftPickCount, rightPickCount, leftPickRate, dPrime, criterion,
                targetTopN, targetTopCorrect, targetTopAccuracy,
                targetBottomN, targetBottomCorrect, targetBottomAccuracy);
    }
}
