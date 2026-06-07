package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.dto.AttemptResponse;
import io.github.nilsfjp.ideophonearena.dto.LeaderboardEntryResponse;
import io.github.nilsfjp.ideophonearena.exception.ResourceNotFoundException;
import io.github.nilsfjp.ideophonearena.mapper.GameMapper;
import io.github.nilsfjp.ideophonearena.model.AppUser;
import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import io.github.nilsfjp.ideophonearena.repository.LeaderboardEntryProjection;
import io.github.nilsfjp.ideophonearena.repository.PlayerAnswerRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScoreService {

    private static final int DEFAULT_LEADERBOARD_LIMIT = 10;
    private static final int DEFAULT_ATTEMPT_LIMIT = 20;

    private final AppUserRepository appUserRepository;
    private final PlayerAnswerRepository playerAnswerRepository;
    private final GameMapper gameMapper;

    public ScoreService(AppUserRepository appUserRepository, PlayerAnswerRepository playerAnswerRepository,
            GameMapper gameMapper) {
        this.appUserRepository = appUserRepository;
        this.playerAnswerRepository = playerAnswerRepository;
        this.gameMapper = gameMapper;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> getLeaderboard() {
        return playerAnswerRepository.findLeaderboard(PageRequest.of(0, DEFAULT_LEADERBOARD_LIMIT))
                .stream()
                .map(this::toLeaderboardEntryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttemptResponse> getMyAttempts(UserDetails userDetails) {
        AppUser user = appUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        return playerAnswerRepository.findBySessionUserIdOrderByAnsweredAtDesc(
                        user.getId(),
                        PageRequest.of(0, DEFAULT_ATTEMPT_LIMIT)
                )
                .stream()
                .map(gameMapper::toAttemptResponse)
                .toList();
    }

    private LeaderboardEntryResponse toLeaderboardEntryResponse(LeaderboardEntryProjection projection) {
        long totalAnswered = valueOrZero(projection.getTotalAnswers());
        long totalCorrect = valueOrZero(projection.getCorrectAnswers());
        double accuracy = totalAnswered == 0 ? 0.0 : (double) totalCorrect / totalAnswered;
        return new LeaderboardEntryResponse(projection.getUsername(), totalAnswered, totalCorrect, accuracy);
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
