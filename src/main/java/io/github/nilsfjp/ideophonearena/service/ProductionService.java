package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.dto.ProductionPageResponse;
import io.github.nilsfjp.ideophonearena.dto.ProductionPromptResponse;
import io.github.nilsfjp.ideophonearena.dto.ProductionRequest;
import io.github.nilsfjp.ideophonearena.dto.ProductionResponse;
import io.github.nilsfjp.ideophonearena.exception.ConflictException;
import io.github.nilsfjp.ideophonearena.exception.ForbiddenException;
import io.github.nilsfjp.ideophonearena.exception.ResourceNotFoundException;
import io.github.nilsfjp.ideophonearena.mapper.ProductionMapper;
import io.github.nilsfjp.ideophonearena.model.AppUser;
import io.github.nilsfjp.ideophonearena.model.GameSession;
import io.github.nilsfjp.ideophonearena.model.Production;
import io.github.nilsfjp.ideophonearena.model.Word;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import io.github.nilsfjp.ideophonearena.repository.GameSessionRepository;
import io.github.nilsfjp.ideophonearena.repository.ProductionRepository;
import io.github.nilsfjp.ideophonearena.repository.WordRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductionService {

    private static final int MAX_PRODUCTIONS_PAGE_SIZE = 50;

    private static final String ALREADY_PRODUCED = "This ideophone has already been produced by this user";

    // The prompt round-robin: one word per modality per step, skipping modalities that have
    // run out. HAPTIC sits after VISUAL, matching the perception ladder's floor hierarchy.
    // Extending the cycle is a one-line change here.
    private static final List<Modality> PROMPT_CYCLE =
            List.of(Modality.AUDITORY, Modality.VISUAL, Modality.HAPTIC, Modality.INTEROCEPTIVE);

    private final AppUserRepository appUserRepository;
    private final WordRepository wordRepository;
    private final GameSessionRepository gameSessionRepository;
    private final ProductionRepository productionRepository;
    private final PhonologyService phonologyService;
    private final ProductionMapper productionMapper;

    public ProductionService(AppUserRepository appUserRepository, WordRepository wordRepository,
            GameSessionRepository gameSessionRepository, ProductionRepository productionRepository,
            PhonologyService phonologyService, ProductionMapper productionMapper) {
        this.appUserRepository = appUserRepository;
        this.wordRepository = wordRepository;
        this.gameSessionRepository = gameSessionRepository;
        this.productionRepository = productionRepository;
        this.phonologyService = phonologyService;
        this.productionMapper = productionMapper;
    }

    // Deterministic and stateless: the caller's production count is the cycle cursor, so no
    // column tracks where they are. Terminates because every step either serves an unproduced
    // word or finds all four modalities exhausted.
    @Transactional(readOnly = true)
    public ProductionPromptResponse getNextPrompt(UserDetails userDetails) {
        AppUser user = getCurrentUser(userDetails);
        long produced = productionRepository.countByUserId(user.getId());
        for (int offset = 0; offset < PROMPT_CYCLE.size(); offset++) {
            Modality modality = PROMPT_CYCLE.get((int) ((produced + offset) % PROMPT_CYCLE.size()));
            List<Word> candidates = productionRepository.findNextUnproducedByModality(
                    user.getId(), modality, PageRequest.of(0, 1));
            if (!candidates.isEmpty()) {
                return productionMapper.toPromptResponse(candidates.get(0));
            }
        }
        return productionMapper.toCompletedPromptResponse();
    }

    @Transactional
    public ProductionResponse createProduction(UserDetails userDetails, ProductionRequest request) {
        AppUser user = getCurrentUser(userDetails);

        // Parse first, so an unparseable form never reaches the table: the one attempt this
        // user gets on this word survives a typo.
        String rawInput = request.getInput().trim().toLowerCase(Locale.ROOT);
        String normalizedForm = phonologyService.normalizeInput(rawInput, PhonologyProfile.JAPANESE);

        Word word = wordRepository.findById(request.getIdeophoneId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ideophone not found: " + request.getIdeophoneId()));
        GameSession session = resolveSession(user, request.getSessionUuid());

        if (productionRepository.existsByUserIdAndWordId(user.getId(), word.getId())) {
            throw new ConflictException(ALREADY_PRODUCED);
        }

        PhonologyFeatures yours = phonologyService.features(normalizedForm, PhonologyProfile.JAPANESE);
        // The inventory path: words.romaji already carries the N/Q markers, so it must not
        // pass through normalizeInput().
        PhonologyFeatures target = phonologyService.features(word.getRomaji(), PhonologyProfile.JAPANESE);
        int similarityScore = phonologyService.score(yours, target);

        Production production = new Production(user, word, session, rawInput, normalizedForm,
                (short) similarityScore, PhonologyService.SCORER_VERSION, request.getResponseTimeMs());
        try {
            // Flush now so a concurrent duplicate hits UNIQUE(user_id, word_id) here
            // instead of surfacing at commit as a 500.
            productionRepository.saveAndFlush(production);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(ALREADY_PRODUCED);
        }
        return productionMapper.toResponse(production, yours, target);
    }

    @Transactional(readOnly = true)
    public ProductionPageResponse getMyProductions(UserDetails userDetails, int page, int size) {
        AppUser user = getCurrentUser(userDetails);
        // Out-of-range params are clamped rather than rejected; the response metadata
        // reports the effective values (mirrors RatingService).
        int effectivePage = Math.max(page, 0);
        int effectiveSize = Math.min(Math.max(size, 1), MAX_PRODUCTIONS_PAGE_SIZE);
        return productionMapper.toPageResponse(
                productionRepository.findByUserIdOrderByCreatedAtDescIdDesc(user.getId(),
                        PageRequest.of(effectivePage, effectiveSize)));
    }

    // Provenance only, and deliberately agnostic to gameMode: a player who finishes a Touch
    // floor and then mints a word they just met is a valid flow.
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
