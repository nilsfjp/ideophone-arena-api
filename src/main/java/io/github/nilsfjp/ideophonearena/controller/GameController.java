package io.github.nilsfjp.ideophonearena.controller;

import io.github.nilsfjp.ideophonearena.dto.AnswerResultResponse;
import io.github.nilsfjp.ideophonearena.dto.GameSessionResponse;
import io.github.nilsfjp.ideophonearena.dto.LadderFloorsResponse;
import io.github.nilsfjp.ideophonearena.dto.RoundResponse;
import io.github.nilsfjp.ideophonearena.dto.StartSessionRequest;
import io.github.nilsfjp.ideophonearena.dto.SubmitAnswerRequest;
import io.github.nilsfjp.ideophonearena.service.GameService;
import io.github.nilsfjp.ideophonearena.service.LadderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;
    private final LadderService ladderService;

    public GameController(GameService gameService, LadderService ladderService) {
        this.gameService = gameService;
        this.ladderService = ladderService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<GameSessionResponse> startSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody StartSessionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gameService.startSession(userDetails, request));
    }

    // Perception Ladder overview: floors in climb order, each with its easy->hard pairs and
    // the caller's own progress (cleared + best score). A floor appears only once it has
    // served trials.
    @GetMapping("/ladder/floors")
    public ResponseEntity<LadderFloorsResponse> getLadderFloors(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(ladderService.getFloors(userDetails));
    }

    @GetMapping("/sessions/{sessionUuid}/rounds/next")
    public ResponseEntity<RoundResponse> getNextRound(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String sessionUuid
    ) {
        return ResponseEntity.ok(gameService.getNextRound(userDetails, sessionUuid));
    }

    @PostMapping("/sessions/{sessionUuid}/answers")
    public ResponseEntity<AnswerResultResponse> submitAnswer(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String sessionUuid,
            @Valid @RequestBody SubmitAnswerRequest request
    ) {
        return ResponseEntity.ok(gameService.submitAnswer(userDetails, sessionUuid, request));
    }
}
