package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.dto.AttemptResponse;
import io.github.nilsfjp.ideophonearena.dto.LeaderboardPageResponse;
import io.github.nilsfjp.ideophonearena.exception.ResourceNotFoundException;
import io.github.nilsfjp.ideophonearena.mapper.GameMapper;
import io.github.nilsfjp.ideophonearena.model.AppUser;
import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import io.github.nilsfjp.ideophonearena.repository.PlayerAnswerRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScoreService {

    private static final int MAX_LEADERBOARD_PAGE_SIZE = 50;
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
    public LeaderboardPageResponse getLeaderboard(int page, int size) {
        // Out-of-range params are clamped rather than rejected; the response
        // metadata reports the effective values. PageRequest stays unsorted so
        // the JPQL aggregate ordering is authoritative.
        int effectivePage = Math.max(page, 0);
        int effectiveSize = Math.min(Math.max(size, 1), MAX_LEADERBOARD_PAGE_SIZE);
        return gameMapper.toLeaderboardPageResponse(
                playerAnswerRepository.findLeaderboard(PageRequest.of(effectivePage, effectiveSize)));
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
}
