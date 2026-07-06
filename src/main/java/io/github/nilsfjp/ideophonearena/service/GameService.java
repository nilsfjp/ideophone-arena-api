package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.dto.AnswerResultResponse;
import io.github.nilsfjp.ideophonearena.dto.GameSessionResponse;
import io.github.nilsfjp.ideophonearena.dto.RoundResponse;
import io.github.nilsfjp.ideophonearena.dto.StartSessionRequest;
import io.github.nilsfjp.ideophonearena.dto.SubmitAnswerRequest;
import io.github.nilsfjp.ideophonearena.exception.BadRequestException;
import io.github.nilsfjp.ideophonearena.exception.ForbiddenException;
import io.github.nilsfjp.ideophonearena.exception.ConflictException;
import io.github.nilsfjp.ideophonearena.exception.ResourceNotFoundException;
import io.github.nilsfjp.ideophonearena.mapper.GameMapper;
import io.github.nilsfjp.ideophonearena.model.AppUser;
import io.github.nilsfjp.ideophonearena.model.DerivedRound;
import io.github.nilsfjp.ideophonearena.model.GameSession;
import io.github.nilsfjp.ideophonearena.model.PlayerAnswer;
import io.github.nilsfjp.ideophonearena.model.Presentation;
import io.github.nilsfjp.ideophonearena.model.Trial;
import io.github.nilsfjp.ideophonearena.model.Word;
import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;
import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import io.github.nilsfjp.ideophonearena.repository.GameSessionRepository;
import io.github.nilsfjp.ideophonearena.repository.PlayerAnswerRepository;
import io.github.nilsfjp.ideophonearena.repository.PresentationRepository;
import io.github.nilsfjp.ideophonearena.repository.TrialRepository;
import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {

    private static final int SUPPORTED_DIFFICULTY_LEVEL = 1;
    private static final int PRACTICE_ROUNDS_PER_SESSION = 2;
    private static final Set<ConditionName> SUPPORTED_CONDITION_NAMES = EnumSet.of(
            ConditionName.CONDITION_1_SOKUON,
            ConditionName.CONDITION_2_SOKUON,
            ConditionName.CONDITION_3_SOKUON
    );
    private static final String SESSION_COMPLETE_MESSAGE = "Game session is complete";

    private final AppUserRepository appUserRepository;
    private final GameSessionRepository gameSessionRepository;
    private final TrialRepository trialRepository;
    private final PresentationRepository presentationRepository;
    private final PlayerAnswerRepository playerAnswerRepository;
    private final GameMapper gameMapper;
    private final RoundShuffler roundShuffler;
    private final SecureRandom shuffleSeedSource = new SecureRandom();

    public GameService(AppUserRepository appUserRepository, GameSessionRepository gameSessionRepository,
            TrialRepository trialRepository, PresentationRepository presentationRepository,
            PlayerAnswerRepository playerAnswerRepository, GameMapper gameMapper, RoundShuffler roundShuffler) {
        this.appUserRepository = appUserRepository;
        this.gameSessionRepository = gameSessionRepository;
        this.trialRepository = trialRepository;
        this.presentationRepository = presentationRepository;
        this.playerAnswerRepository = playerAnswerRepository;
        this.gameMapper = gameMapper;
        this.roundShuffler = roundShuffler;
    }

    @Transactional
    public GameSessionResponse startSession(UserDetails userDetails, StartSessionRequest request) {
        AppUser user = getCurrentUser(userDetails);
        Integer difficultyLevel = request.getDifficultyLevel();
        ConditionName conditionName = request.getConditionName();

        validateSupportedStartRequest(conditionName, difficultyLevel);

        GameSession session = new GameSession(user, conditionName, difficultyLevel, request.isIncludePractice(),
                shuffleSeedSource.nextLong());
        return gameMapper.toSessionResponse(gameSessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public RoundResponse getNextRound(UserDetails userDetails, String sessionUuid) {
        AppUser user = getCurrentUser(userDetails);
        GameSession session = getOwnedSession(user, sessionUuid);

        if (session.isIncludePractice()) {
            List<DerivedRound> practiceRounds = derivedPracticeRoundsForSession(session);
            if (session.getPracticeAnswered() < practiceRounds.size()) {
                DerivedRound round = practiceRounds.get(session.getPracticeAnswered());
                return gameMapper.toRoundResponse(session, round, presentationsForRound(session, round));
            }
        }

        List<DerivedRound> rounds = derivedScoredRoundsForSession(session);
        if (rounds.isEmpty()) {
            throw new ResourceNotFoundException("No rounds found for this session");
        }

        Set<Long> answeredTrialIds = new HashSet<>(playerAnswerRepository.findAnsweredTrialIdsBySessionId(
                session.getId()));
        for (DerivedRound round : rounds) {
            if (!answeredTrialIds.contains(round.getTrial().getId())) {
                return gameMapper.toRoundResponse(session, round, presentationsForRound(session, round));
            }
        }

        return gameMapper.toCompletedRoundResponse(session, SESSION_COMPLETE_MESSAGE);
    }

    @Transactional
    public AnswerResultResponse submitAnswer(UserDetails userDetails, String sessionUuid, SubmitAnswerRequest request) {
        AppUser user = getCurrentUser(userDetails);
        GameSession session = getOwnedSession(user, sessionUuid);
        Trial trial = trialRepository.findByIdWithPairingWords(request.getRoundId())
                .orElseThrow(() -> new ResourceNotFoundException("Round not found"));

        // Trials are condition-free (ADR-3): every scored trial belongs to every
        // session, so there is no cross-condition round to reject here.
        if (trial.isPractice()) {
            return submitPracticeAnswer(session, trial, request);
        }
        if (playerAnswerRepository.existsBySessionIdAndTrialId(session.getId(), trial.getId())) {
            throw new ConflictException("This round has already been answered in this session");
        }

        DerivedRound derivedRound = derivedScoredRound(session, trial);
        Word selectedWord = getSelectedWord(trial, request.getSelectedIdeophoneId());
        boolean correct = isCorrectChoice(derivedRound, selectedWord);
        PlayerAnswer answer = new PlayerAnswer(session, trial, selectedWord, derivedRound.getTarget(),
                request.getResponseTimeMs(), correct);
        try {
            // Flush now so a concurrent duplicate hits UNIQUE(session_id, trial_id)
            // here instead of surfacing at commit as a 500.
            playerAnswerRepository.saveAndFlush(answer);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("This round has already been answered in this session");
        }

        long totalAnswered = playerAnswerRepository.countBySessionId(session.getId());
        long totalCorrect = playerAnswerRepository.countBySessionIdAndCorrectTrue(session.getId());

        long totalRounds = trialRepository.countByPracticeFalse();
        if (session.getCompletedAt() == null && totalAnswered == totalRounds) {
            session.complete();
        }

        return gameMapper.toAnswerResultResponse(derivedRound, selectedWord, answer, totalAnswered, totalCorrect);
    }

    // Practice answers return feedback but are never persisted: they do not
    // create PlayerAnswer rows and cannot affect score, completion, or the
    // leaderboard. Only the session's practice cursor advances.
    private AnswerResultResponse submitPracticeAnswer(GameSession session, Trial trial,
            SubmitAnswerRequest request) {
        if (!session.isIncludePractice()) {
            throw new BadRequestException("This session was started without practice rounds");
        }

        List<DerivedRound> practiceRounds = derivedPracticeRoundsForSession(session);
        int roundIndex = -1;
        for (int index = 0; index < practiceRounds.size(); index++) {
            if (practiceRounds.get(index).getTrial().getId().equals(trial.getId())) {
                roundIndex = index;
                break;
            }
        }
        if (roundIndex < 0) {
            throw new BadRequestException("This practice round is not part of this session");
        }
        if (roundIndex < session.getPracticeAnswered()) {
            throw new ConflictException("This practice round has already been answered in this session");
        }
        if (roundIndex > session.getPracticeAnswered()) {
            throw new BadRequestException("Practice rounds must be answered in order");
        }

        DerivedRound derivedRound = practiceRounds.get(roundIndex);
        Word selectedWord = getSelectedWord(trial, request.getSelectedIdeophoneId());
        boolean correct = isCorrectChoice(derivedRound, selectedWord);
        session.recordPracticeAnswer();

        long totalAnswered = playerAnswerRepository.countBySessionId(session.getId());
        long totalCorrect = playerAnswerRepository.countBySessionIdAndCorrectTrue(session.getId());
        return gameMapper.toPracticeAnswerResultResponse(derivedRound, selectedWord, correct, totalAnswered,
                totalCorrect);
    }

    // The session serves the first PRACTICE_ROUNDS_PER_SESSION practice trials
    // in seed order (p0 auditory, p1 visual); only the per-round presentation
    // draws come from the practice stream.
    private List<DerivedRound> derivedPracticeRoundsForSession(GameSession session) {
        List<Trial> practiceTrials = trialRepository.findByPracticeTrueOrderByIdAsc();
        List<Trial> served = practiceTrials.subList(0,
                Math.min(PRACTICE_ROUNDS_PER_SESSION, practiceTrials.size()));
        return roundShuffler.derivePracticeRounds(session.getShuffleSeed(), served);
    }

    private List<DerivedRound> derivedScoredRoundsForSession(GameSession session) {
        List<Trial> trials = trialRepository.findByPracticeFalseOrderByIdAsc();
        return roundShuffler.deriveScoredRounds(session.getShuffleSeed(), trials);
    }

    private DerivedRound derivedScoredRound(GameSession session, Trial trial) {
        for (DerivedRound derived : derivedScoredRoundsForSession(session)) {
            if (derived.getTrial().getId().equals(trial.getId())) {
                return derived;
            }
        }
        throw new BadRequestException("Round does not belong to this session's scored trials");
    }

    // Resolves the two served words' presentations for this session's condition,
    // keyed by word id -- the display_form/script_code the round DTO renders.
    private Map<Long, Presentation> presentationsForRound(GameSession session, DerivedRound round) {
        List<Long> wordIds = List.of(round.getLeft().getId(), round.getRight().getId());
        Map<Long, Presentation> byWordId = new HashMap<>();
        for (Presentation presentation : presentationRepository.findByWordIdInAndConditionName(
                wordIds, session.getConditionName())) {
            byWordId.put(presentation.getWord().getId(), presentation);
        }
        return byWordId;
    }

    private AppUser getCurrentUser(UserDetails userDetails) {
        return appUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private void validateSupportedStartRequest(ConditionName conditionName, Integer difficultyLevel) {
        if (difficultyLevel != SUPPORTED_DIFFICULTY_LEVEL) {
            throw new BadRequestException("Only difficulty level 1 is supported for the current demo");
        }
        if (!SUPPORTED_CONDITION_NAMES.contains(conditionName)) {
            throw new BadRequestException(
                    "Unsupported conditionName: " + conditionName
                            + ". Supported values are CONDITION_1_SOKUON, CONDITION_2_SOKUON, CONDITION_3_SOKUON"
            );
        }
    }

    private GameSession getOwnedSession(AppUser user, String sessionUuid) {
        GameSession session = gameSessionRepository.findBySessionUuid(sessionUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Game session not found"));
        if (!session.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Game session belongs to another user");
        }
        return session;
    }

    // The selected id is a word id (frozen field name: selectedIdeophoneId),
    // matched against the trial's pairing members.
    private Word getSelectedWord(Trial trial, Long selectedWordId) {
        Word wordA = trial.getPairing().getWordA();
        Word wordB = trial.getPairing().getWordB();
        if (wordA.getId().equals(selectedWordId)) {
            return wordA;
        }
        if (wordB.getId().equals(selectedWordId)) {
            return wordB;
        }
        throw new BadRequestException("Selected word is not an option for this round");
    }

    // Correctness is judged against the seed-derived target, never against
    // trials.correct_word_id (which documents the thesis target).
    private boolean isCorrectChoice(DerivedRound derivedRound, Word selectedWord) {
        return derivedRound.getTarget().getId().equals(selectedWord.getId());
    }
}
