package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import java.util.List;
import org.springframework.stereotype.Component;

// The static server-side ordering map for the Perception Ladder (28A gate; ARCHITECTURE
// ADR-6/section 8/section 11). Floors are NOT schema: this holds the McLean hierarchy order
// (Sound -> Sight -> Touch -> Inner states) and, within each floor, the easy->hard pair-code
// sequence -- thesis-facts section 8 for A/V/I and SPEC-four-floor-ladder section 4 for the
// Touch floor.
//
// Membership and order both come from here (keyed by pair_code), which is why the live
// A/V/I expansion pairs -- same modality as the thesis floors -- do not leak into a floor.
// A floor's PRESENCE in the API is data-driven: LadderService only surfaces a floor whose
// codes have served trials, so a new floor slots in at its hierarchy position with zero
// code change once its trials are seeded. When M2's effectiveDifficulty() lands, this seam
// can read it over pairings with no API change (ARCHITECTURE section 11) -- not built here.
@Component
public class LadderFloors {

    private static final List<Floor> HIERARCHY = List.of(
            new Floor(Modality.AUDITORY, List.of("a9", "a7", "a2", "a0", "a3", "a6", "a4", "a8", "a1", "a5")),
            new Floor(Modality.VISUAL, List.of("v2", "v8", "v0", "v7", "v5", "v1", "v3", "v6", "v9", "v4")),
            new Floor(Modality.HAPTIC, List.of("exp-h1", "exp-h5", "exp-h4", "exp-h6")),
            new Floor(Modality.INTEROCEPTIVE, List.of("i3", "i5", "i9", "i0", "i4", "i1", "i6", "i7", "i8", "i2")));

    // Floors in climb/hierarchy order. LadderService filters this to the ones with trials.
    public List<Floor> hierarchy() {
        return HIERARCHY;
    }

    // A floor's easy->hard pair codes, or an empty list if the modality is not a ladder floor.
    public List<String> pairCodesInOrder(Modality modality) {
        return HIERARCHY.stream()
                .filter(floor -> floor.getModality() == modality)
                .findFirst()
                .map(Floor::getPairCodes)
                .orElse(List.of());
    }

    public boolean isLadderFloor(Modality modality) {
        return HIERARCHY.stream().anyMatch(floor -> floor.getModality() == modality);
    }

    // One floor's static definition: its modality and its ordered pair codes. The last
    // code is the final rung (a5 / v4 / exp-h6 / i2 -- the at-or-below-chance pairs).
    public static final class Floor {

        private final Modality modality;
        private final List<String> pairCodes;

        private Floor(Modality modality, List<String> pairCodes) {
            this.modality = modality;
            this.pairCodes = pairCodes;
        }

        public Modality getModality() {
            return modality;
        }

        public List<String> getPairCodes() {
            return pairCodes;
        }

        public String getFinalRungPairCode() {
            return pairCodes.get(pairCodes.size() - 1);
        }
    }
}
