package io.github.nilsfjp.ideophonearena.dto;

import java.util.List;

// The Perception Ladder overview: floors in climb/hierarchy order (Sound -> Sight ->
// Touch -> Inner states). Only floors that have served trials appear, so the array is
// data-driven -- a new floor slots in at its hierarchy position with no shape change.
public class LadderFloorsResponse {

    private List<LadderFloorResponse> floors;

    public LadderFloorsResponse(List<LadderFloorResponse> floors) {
        this.floors = floors;
    }

    public List<LadderFloorResponse> getFloors() {
        return floors;
    }
}
