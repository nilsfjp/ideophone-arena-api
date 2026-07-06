package io.github.nilsfjp.ideophonearena.controller;

import io.github.nilsfjp.ideophonearena.dto.DivergenceResponse;
import io.github.nilsfjp.ideophonearena.dto.PositionBiasResponse;
import io.github.nilsfjp.ideophonearena.dto.RatingDistributionsResponse;
import io.github.nilsfjp.ideophonearena.service.ResearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/research")
@Tag(name = "Research", description = "Public read-only experiment aggregates")
public class ResearchController {

    private final ResearchService researchService;

    public ResearchController(ResearchService researchService) {
        this.researchService = researchService;
    }

    @GetMapping("/divergence")
    @Operation(summary = "Per-ideophone guess accuracy vs mean iconicity rating")
    public ResponseEntity<List<DivergenceResponse>> getDivergence() {
        return ResponseEntity.ok(researchService.getDivergence());
    }

    @GetMapping("/thesis/divergence")
    @Operation(summary = "Thesis-cohort divergence (NIL-54): the vendored per-pair experiment, in-DB")
    public ResponseEntity<List<DivergenceResponse>> getThesisDivergence() {
        return ResponseEntity.ok(researchService.getThesisDivergence());
    }

    @GetMapping("/rating-distributions")
    @Operation(summary = "Per-modality distribution of the 1-7 iconicity rating values")
    public ResponseEntity<RatingDistributionsResponse> getRatingDistributions() {
        return ResponseEntity.ok(researchService.getRatingDistributions());
    }

    @GetMapping("/position-bias")
    @Operation(summary = "SDT fairness check on the forced choice: left/right and target-position bias")
    public ResponseEntity<PositionBiasResponse> getPositionBias() {
        return ResponseEntity.ok(researchService.getPositionBias());
    }
}
