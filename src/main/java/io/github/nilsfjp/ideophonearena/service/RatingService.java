package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.dto.RatableWordPageResponse;
import io.github.nilsfjp.ideophonearena.dto.RatingPageResponse;
import io.github.nilsfjp.ideophonearena.dto.RatingRequest;
import io.github.nilsfjp.ideophonearena.dto.RatingResponse;
import io.github.nilsfjp.ideophonearena.exception.ConflictException;
import io.github.nilsfjp.ideophonearena.exception.ForbiddenException;
import io.github.nilsfjp.ideophonearena.exception.ResourceNotFoundException;
import io.github.nilsfjp.ideophonearena.mapper.RatingMapper;
import io.github.nilsfjp.ideophonearena.model.AppUser;
import io.github.nilsfjp.ideophonearena.model.GameSession;
import io.github.nilsfjp.ideophonearena.model.Rating;
import io.github.nilsfjp.ideophonearena.model.Word;
import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import io.github.nilsfjp.ideophonearena.repository.GameSessionRepository;
import io.github.nilsfjp.ideophonearena.repository.PlayerAnswerRepository;
import io.github.nilsfjp.ideophonearena.repository.RatingRepository;
import io.github.nilsfjp.ideophonearena.repository.WordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingService {

    private static final int MAX_RATINGS_PAGE_SIZE = 50;

    private final AppUserRepository appUserRepository;
    private final WordRepository wordRepository;
    private final GameSessionRepository gameSessionRepository;
    private final RatingRepository ratingRepository;
    private final PlayerAnswerRepository playerAnswerRepository;
    private final RatingMapper ratingMapper;

    public RatingService(AppUserRepository appUserRepository, WordRepository wordRepository,
            GameSessionRepository gameSessionRepository, RatingRepository ratingRepository,
            PlayerAnswerRepository playerAnswerRepository, RatingMapper ratingMapper) {
        this.appUserRepository = appUserRepository;
        this.wordRepository = wordRepository;
        this.gameSessionRepository = gameSessionRepository;
        this.ratingRepository = ratingRepository;
        this.playerAnswerRepository = playerAnswerRepository;
        this.ratingMapper = ratingMapper;
    }

    @Transactional
    public RatingResponse createRating(UserDetails userDetails, RatingRequest request) {
        AppUser user = getCurrentUser(userDetails);
        Word word = wordRepository.findById(request.getIdeophoneId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ideophone not found: " + request.getIdeophoneId()));
        GameSession session = resolveSession(user, request.getSessionUuid());

        // Word-keyed uniqueness (ADR-0): one rating per word per user holds across
        // conditions -- the grain the instrument means.
        if (ratingRepository.existsByUserIdAndWordId(user.getId(), word.getId())) {
            throw new ConflictException("This ideophone has already been rated by this user");
        }

        Rating rating = new Rating(user, word, session, request.getRating().shortValue(),
                request.getResponseTimeMs());
        try {
            // Flush now so a concurrent duplicate hits UNIQUE(user_id, word_id)
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
                ratingRepository.findByUserIdOrderByRatedAtDescIdDesc(user.getId(),
                        PageRequest.of(effectivePage, effectiveSize)));
    }

    // The thesis contamination rule, enforced server-side: rating shows a
    // word's meaning, so only words whose mapping an answered Choosing round
    // already revealed are ratable. Practice words never qualify (their
    // answers are never persisted); already-rated words drop out.
    @Transactional(readOnly = true)
    public RatableWordPageResponse getMyRatableWords(UserDetails userDetails, int page, int size) {
        AppUser user = getCurrentUser(userDetails);
        int effectivePage = Math.max(page, 0);
        int effectiveSize = Math.min(Math.max(size, 1), MAX_RATINGS_PAGE_SIZE);
        return ratingMapper.toRatableWordPageResponse(
                playerAnswerRepository.findRatableWordsByUserId(user.getId(),
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
