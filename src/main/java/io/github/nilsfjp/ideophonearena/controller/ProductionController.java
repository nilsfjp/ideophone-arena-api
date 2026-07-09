package io.github.nilsfjp.ideophonearena.controller;

import io.github.nilsfjp.ideophonearena.dto.ProductionPageResponse;
import io.github.nilsfjp.ideophonearena.dto.ProductionPromptResponse;
import io.github.nilsfjp.ideophonearena.dto.ProductionRequest;
import io.github.nilsfjp.ideophonearena.dto.ProductionResponse;
import io.github.nilsfjp.ideophonearena.service.ProductionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Production", description = "Free-form entry: invent a word for a meaning and score it against the real one")
public class ProductionController {

    private final ProductionService productionService;

    public ProductionController(ProductionService productionService) {
        this.productionService = productionService;
    }

    @GetMapping("/productions/next")
    @Operation(summary = "Next meaning the caller has not produced a word for yet")
    public ResponseEntity<ProductionPromptResponse> getNextPrompt(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(productionService.getNextPrompt(userDetails));
    }

    @PostMapping("/productions")
    @Operation(summary = "Submit an invented romaji word for one ideophone and reveal the real one")
    public ResponseEntity<ProductionResponse> createProduction(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProductionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productionService.createProduction(userDetails, request));
    }

    @GetMapping("/game/me/productions")
    @Operation(summary = "List the caller's own productions, most recent first (paginated)")
    public ResponseEntity<ProductionPageResponse> getMyProductions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(productionService.getMyProductions(userDetails, page, size));
    }
}
