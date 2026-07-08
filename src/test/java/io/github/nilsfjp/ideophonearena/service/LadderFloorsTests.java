package io.github.nilsfjp.ideophonearena.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import java.util.List;
import org.junit.jupiter.api.Test;

class LadderFloorsTests {

    private final LadderFloors ladderFloors = new LadderFloors();

    @Test
    void hierarchyIsSoundSightTouchInnerStates() {
        List<Modality> order = ladderFloors.hierarchy().stream()
                .map(LadderFloors.Floor::getModality)
                .toList();
        assertEquals(
                List.of(Modality.AUDITORY, Modality.VISUAL, Modality.HAPTIC, Modality.INTEROCEPTIVE),
                order,
                "floor order is the McLean hierarchy: Sound -> Sight -> Touch -> Inner states");
    }

    @Test
    void withinFloorOrderMatchesThesisFactsSection8AndFourFloorSpec() {
        // Pinned to the literal sequences (thesis-facts §8 for A/V/I, SPEC-four-floor-ladder
        // §4 for HAPTIC) -- the tie-break rule that reproduces them is a convention, so we
        // assert the literals rather than re-derive them.
        assertEquals(List.of("a9", "a7", "a2", "a0", "a3", "a6", "a4", "a8", "a1", "a5"),
                ladderFloors.pairCodesInOrder(Modality.AUDITORY));
        assertEquals(List.of("v2", "v8", "v0", "v7", "v5", "v1", "v3", "v6", "v9", "v4"),
                ladderFloors.pairCodesInOrder(Modality.VISUAL));
        assertEquals(List.of("i3", "i5", "i9", "i0", "i4", "i1", "i6", "i7", "i8", "i2"),
                ladderFloors.pairCodesInOrder(Modality.INTEROCEPTIVE));
        assertEquals(List.of("exp-h1", "exp-h5", "exp-h4", "exp-h6"),
                ladderFloors.pairCodesInOrder(Modality.HAPTIC));
    }

    @Test
    void finalRungIsTheLastAtOrBelowChancePairPerFloor() {
        for (LadderFloors.Floor floor : ladderFloors.hierarchy()) {
            String expected = switch (floor.getModality()) {
                case AUDITORY -> "a5";
                case VISUAL -> "v4";
                case HAPTIC -> "exp-h6";
                case INTEROCEPTIVE -> "i2";
                default -> null;
            };
            assertEquals(expected, floor.getFinalRungPairCode(), floor.getModality() + " final rung");
        }
    }

    @Test
    void onlyTheFourLadderModalitiesAreFloors() {
        assertTrue(ladderFloors.isLadderFloor(Modality.AUDITORY));
        assertTrue(ladderFloors.isLadderFloor(Modality.HAPTIC));
        assertFalse(ladderFloors.isLadderFloor(Modality.TACTILE));
        assertFalse(ladderFloors.isLadderFloor(Modality.MOTION));
        assertTrue(ladderFloors.pairCodesInOrder(Modality.TACTILE).isEmpty());
    }
}
