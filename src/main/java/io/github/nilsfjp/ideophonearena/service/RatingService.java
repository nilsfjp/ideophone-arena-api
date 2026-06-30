package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.dto.RatingPageResponse;
import io.github.nilsfjp.ideophonearena.dto.RatingRequest;
import io.github.nilsfjp.ideophonearena.dto.RatingResponse;
import io.github.nilsfjp.ideophonearena.exception.ConflictException;
import io.github.nilsfjp.ideophonearena.exception.ForbiddenException;
import io.github.nilsfjp.ideophonearena.exception.ResourceNotFoundException;
import io.github.nilsfjp.ideophonearena.mapper.RatingMapper;
import io.github.nilsfjp.ideophonearena.model.AppUser;
import io.github.nilsfjp.ideophonearena.model.GameSession;
import io.github.nilsfjp.ideophonearena.model.Ideophone;
import io.github.nilsfjp.ideophonearena.model.Rating;
import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import io.github.nilsfjp.ideophonearena.repository.GameSessionRepository;
import io.github.nilsfjp.ideophonearena.repository.IdeophoneRepository;
import io.github.nilsfjp.ideophonearena.repository.RatingRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingService {

    private static final int MAX_RATINGS_PAGE_SIZE = 50;

    private final AppUserRepository appUserRepository;
    private final IdeophoneRepository ideophoneRepository;
    private final GameSessionRepository gameSessionRepository;
    private final RatingRepository ratingRepository;
    private final RatingMapper ratingMapper;

    public RatingService(AppUserRepository appUserRepository, IdeophoneRepository ideophoneRepository,
            GameSessionRepository gameSessionRepository, RatingRepository ratingRepository,
            RatingMapper ratingMapper) {
        this.appUserRepository = appUserRepository;
        this.ideophoneRepository = ideophoneRepository;
        this.gameSessionRepository = gameSessionRepository;
        this.ratingRepository = ratingRepository;
        this.ratingMapper = ratingMapper;
    }

    @Transactional
    public RatingResponse createRating(UserDetails userDetails, RatingRequest request) {
        AppUser user = getCurrentUser(userDetails);
        Ideophone ideophone = ideophoneRepository.findById(request.getIdeophoneId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ideophone not found: " + request.getIdeophoneId()));
        GameSession session = resolveSession(user, request.getSessionUuid());

        if (ratingRepository.existsByUserIdAndIdeophoneId(user.getId(), ideophone.getId())) {
            throw new ConflictException("This ideophone has already been rated by this user");
        }

        Rating rating = new Rating(user, ideophone, session, request.getRating().shortValue(),
                request.getResponseTimeMs());
        try {
            // Flush now so a concurrent duplicate hits UNIQUE(user_id, ideophone_id)
            // here instead of surfacing at commit as a 500.
            ratingRepository.saveAndFlush(rating);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("This ideophone has already been rated by this user");
        }
        return ratingMapper.toResponse(rating);
    }

    @Transactional(readOnly = true)
    public RatingPageResponse getMyRatings(UserDetails userDetails, int page, int size) {
        AppUser user = getCurrentUser(userDetails);
        // Out-of-range params are clamped rather than rejected; the response
        // metadata reports the effective values (mirrors ScoreService).
        int effectivePage = Math.max(page, 0);
        int effectiveSize = Math.min(Math.max(size, 1), MAX_RATINGS_PAGE_SIZE);
        return ratingMapper.toPageResponse(
                ratingRepository.findByUserIdOrderByRatedAtDesc(user.getId(),
                        PageRequest.of(effectivePage, effectiveSize)));
    }

    private GameSession resolveSession(AppUser user, String sessionUuid) {
        if (sessionUuid == null || sessionUuid.isBlank()) {
            return null;
        }
        GameSession session = gameSessionRepository.findBySessionUuid(sessionUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Game session not found"));
        if (!session.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Game session belongs to another user");
        }
        return session;
    }

    private AppUser getCurrentUser(UserDetails userDetails) {
        return appUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}
