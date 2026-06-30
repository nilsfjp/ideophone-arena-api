package io.github.nilsfjp.ideophonearena.mapper;

import io.github.nilsfjp.ideophonearena.dto.DivergenceResponse;
import io.github.nilsfjp.ideophonearena.model.Ideophone;
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
                ideophone.getGloss(),
                modality,
                guessAccuracy,
                guessCount,
                meanRating,
                ratingCount);
    }
}
