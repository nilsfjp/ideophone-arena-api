package io.github.nilsfjp.ideophonearena.controller;

import io.github.nilsfjp.ideophonearena.dto.AttemptResponse;
import io.github.nilsfjp.ideophonearena.dto.LeaderboardEntryResponse;
import io.github.nilsfjp.ideophonearena.service.ScoreService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ScoreController {

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @GetMapping("/game/me/attempts")
    public ResponseEntity<List<AttemptResponse>> getMyAttempts(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(scoreService.getMyAttempts(userDetails));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntryResponse>> getLeaderboard() {
        return ResponseEntity.ok(scoreService.getLeaderboard());
    }
}
