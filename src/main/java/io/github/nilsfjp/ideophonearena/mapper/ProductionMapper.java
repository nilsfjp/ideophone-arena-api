package io.github.nilsfjp.ideophonearena.mapper;

import io.github.nilsfjp.ideophonearena.dto.FeatureMatchResponse;
import io.github.nilsfjp.ideophonearena.dto.ProductionEntryResponse;
import io.github.nilsfjp.ideophonearena.dto.ProductionPageResponse;
import io.github.nilsfjp.ideophonearena.dto.ProductionPromptResponse;
import io.github.nilsfjp.ideophonearena.dto.ProductionResponse;
import io.github.nilsfjp.ideophonearena.dto.ProductionTargetResponse;
import io.github.nilsfjp.ideophonearena.model.Production;
import io.github.nilsfjp.ideophonearena.model.Word;
import io.github.nilsfjp.ideophonearena.service.PhonologyFeatures;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class ProductionMapper {

    private static final String STIMULUS_URL_PREFIX = "/stimuli/";

    // Presentational only: the scorer keeps the full-precision ratio.
    private static final int RATIO_DISPLAY_SCALE = 2;

    public ProductionPromptResponse toPromptResponse(Word word) {
        return new ProductionPromptResponse(false, word.getId(), word.getGloss(),
                word.getModality() == null ? null : word.getModality().name());
    }

    public ProductionPromptResponse toCompletedPromptResponse() {
        return new ProductionPromptResponse(true, null, null, null);
    }

    public ProductionResponse toResponse(Production production, PhonologyFeatures yours,
            PhonologyFeatures target) {
        Word word = production.getWord();
        return new ProductionResponse(
                production.getId(),
                word.getId(),
                production.getRawInput(),
                production.getSimilarityScore(),
                toFeatureMatches(yours, target),
                new ProductionTargetResponse(
                        word.getCanonicalForm(),
                        word.getRomaji(),
                        word.getGloss(),
                        STIMULUS_URL_PREFIX + word.getStimulusFile()));
    }

    // The seven chips of SPEC-view-designs section 8.3, in their frozen order. The client
    // decides which to show; a shared absence is still a match.
    private List<FeatureMatchResponse> toFeatureMatches(PhonologyFeatures yours, PhonologyFeatures target) {
        return List.of(
                flag("redup", yours.redup(), target.redup()),
                flag("sokuon", yours.sokuon(), target.sokuon()),
                flag("finalN", yours.finalN(), target.finalN()),
                flag("riSuffix", yours.riSuffix(), target.riSuffix()),
                flag("voicedOnset", yours.voicedOnset(), target.voicedOnset()),
                new FeatureMatchResponse("heavyVowelRatio",
                        yours.heavyVowelRatio().setScale(RATIO_DISPLAY_SCALE, RoundingMode.HALF_EVEN),
                        target.heavyVowelRatio().setScale(RATIO_DISPLAY_SCALE, RoundingMode.HALF_EVEN),
                        yours.heavyVowelRatio().compareTo(target.heavyVowelRatio()) == 0),
                new FeatureMatchResponse("moraCount", yours.moraCount(), target.moraCount(),
                        yours.moraCount() == target.moraCount()));
    }

    private FeatureMatchResponse flag(String feature, boolean yours, boolean target) {
        return new FeatureMatchResponse(feature, yours, target, yours == target);
    }

    public ProductionEntryResponse toEntryResponse(Production production) {
        return new ProductionEntryResponse(
                production.getId(),
                production.getWord().getId(),
                production.getRawInput(),
                production.getSimilarityScore(),
                production.getCreatedAt());
    }

    public ProductionPageResponse toPageResponse(Page<Production> page) {
        List<ProductionEntryResponse> entries = page.getContent().stream()
                .map(this::toEntryResponse)
                .toList();
        return new ProductionPageResponse(entries, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
