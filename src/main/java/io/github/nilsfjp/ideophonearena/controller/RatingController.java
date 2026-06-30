package io.github.nilsfjp.ideophonearena.controller;

import io.github.nilsfjp.ideophonearena.dto.RatingPageResponse;
import io.github.nilsfjp.ideophonearena.dto.RatingRequest;
import io.github.nilsfjp.ideophonearena.dto.RatingResponse;
import io.github.nilsfjp.ideophonearena.service.RatingService;
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
@Tag(name = "Ratings", description = "Iconicity ratings: submit a 1-7 rating and list your own")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping("/ratings")
    @Operation(summary = "Submit a 1-7 iconicity rating for one ideophone")
    public ResponseEntity<RatingResponse> createRating(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody RatingRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ratingService.createRating(userDetails, request));
    }

    @GetMapping("/game/me/ratings")
    @Operation(summary = "List the caller's own ratings, most recent first (paginated)")
    public ResponseEntity<RatingPageResponse> getMyRatings(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ratingService.getMyRatings(userDetails, page, size));
    }
}
