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
import io.github.nilsfjp.ideophonearena.model.ArenaRound;
import io.github.nilsfjp.ideophonearena.model.DerivedRound;
import io.github.nilsfjp.ideophonearena.model.GameSession;
import io.github.nilsfjp.ideophonearena.model.Ideophone;
import io.github.nilsfjp.ideophonearena.model.PlayerAnswer;
import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;
import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import io.github.nilsfjp.ideophonearena.repository.ArenaRoundRepository;
import io.github.nilsfjp.ideophonearena.repository.GameSessionRepository;
import io.github.nilsfjp.ideophonearena.repository.PlayerAnswerRepository;
import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
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
    private final ArenaRoundRepository arenaRoundRepository;
    private final PlayerAnswerRepository playerAnswerRepository;
    private final GameMapper gameMapper;
    private final RoundShuffler roundShuffler;
    private final SecureRandom shuffleSeedSource = new SecureRandom();

    public GameService(AppUserRepository appUserRepository, GameSessionRepository gameSessionRepository,
            ArenaRoundRepository arenaRoundRepository, PlayerAnswerRepository playerAnswerRepository,
            GameMapper gameMapper, RoundShuffler roundShuffler) {
        this.appUserRepository = appUserRepository;
        this.gameSessionRepository = gameSessionRepository;
        this.arenaRoundRepository = arenaRoundRepository;
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
                return gameMapper.toRoundResponse(session, practiceRounds.get(session.getPracticeAnswered()));
            }
        }

        List<DerivedRound> rounds = derivedScoredRoundsForSession(session);
        if (rounds.isEmpty()) {
            throw new ResourceNotFoundException("No rounds found for this session");
        }

        Set<Long> answeredRoundIds = new HashSet<>(playerAnswerRepository.findAnsweredRoundIdsBySessionId(
                session.getId()));
        for (DerivedRound round : rounds) {
            if (!answeredRoundIds.contains(round.getRound().getId())) {
                return gameMapper.toRoundResponse(session, round);
            }
        }

        return gameMapper.toCompletedRoundResponse(session, SESSION_COMPLETE_MESSAGE);
    }

    @Transactional
    public AnswerResultResponse submitAnswer(UserDetails userDetails, String sessionUuid, SubmitAnswerRequest request) {
        AppUser user = getCurrentUser(userDetails);
        GameSession session = getOwnedSession(user, sessionUuid);
        ArenaRound round = arenaRoundRepository.findByIdWithIdeophones(request.getRoundId())
                .orElseThrow(() -> new ResourceNotFoundException("Round not found"));

        if (round.getConditionName() != session.getConditionName()
                || round.getDifficultyLevel() != session.getDifficultyLevel()) {
            throw new BadRequestException("Round does not belong to this session condition and difficulty");
        }
        if (round.isPractice()) {
            return submitPracticeAnswer(session, round, request);
        }
        if (playerAnswerRepository.existsBySessionIdAndRoundId(session.getId(), round.getId())) {
            throw new ConflictException("This round has already been answered in this session");
        }

        DerivedRound derivedRound = derivedScoredRound(session, round);
        Ideophone selectedIdeophone = getSelectedIdeophone(round, request.getSelectedIdeophoneId());
        boolean correct = isCorrectChoice(derivedRound, selectedIdeophone);
        PlayerAnswer answer = new PlayerAnswer(session, round, selectedIdeophone, derivedRound.getTarget(),
                request.getResponseTimeMs(), correct);
        try {
            // Flush now so a concurrent duplicate hits UNIQUE(session_id, round_id)
            // here instead of surfacing at commit as a 500.
            playerAnswerRepository.saveAndFlush(answer);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("This round has already been answered in this session");
        }

        long totalAnswered = playerAnswerRepository.countBySessionId(session.getId());
        long totalCorrect = playerAnswerRepository.countBySessionIdAndCorrectTrue(session.getId());

        long totalRounds = arenaRoundRepository.countByConditionNameAndDifficultyLevelAndPracticeFalse(
                session.getConditionName(), session.getDifficultyLevel());
        if (session.getCompletedAt() == null && totalAnswered == totalRounds) {
            session.complete();
        }

        return gameMapper.toAnswerResultResponse(derivedRound, selectedIdeophone, answer, totalAnswered, totalCorrect);
    }

    // Practice answers return feedback but are never persisted: they do not
    // create PlayerAnswer rows and cannot affect score, completion, or the
    // leaderboard. Only the session's practice cursor advances.
    private AnswerResultResponse submitPracticeAnswer(GameSession session, ArenaRound round,
            SubmitAnswerRequest request) {
        if (!session.isIncludePractice()) {
            throw new BadRequestException("This session was started without practice rounds");
        }

        List<DerivedRound> practiceRounds = derivedPracticeRoundsForSession(session);
        int roundIndex = -1;
        for (int index = 0; index < practiceRounds.size(); index++) {
            if (practiceRounds.get(index).getRound().getId().equals(round.getId())) {
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
        Ideophone selectedIdeophone = getSelectedIdeophone(round, request.getSelectedIdeophoneId());
        boolean correct = isCorrectChoice(derivedRound, selectedIdeophone);
        session.recordPracticeAnswer();

        long totalAnswered = playerAnswerRepository.countBySessionId(session.getId());
        long totalCorrect = playerAnswerRepository.countBySessionIdAndCorrectTrue(session.getId());
        return gameMapper.toPracticeAnswerResultResponse(derivedRound, selectedIdeophone, correct, totalAnswered,
                totalCorrect);
    }

    // The session serves the first PRACTICE_ROUNDS_PER_SESSION practice rounds
    // of its condition, in seed order (p0 auditory, p1 visual); only the
    // per-round presentation draws come from the practice stream.
    private List<DerivedRound> derivedPracticeRoundsForSession(GameSession session) {
        List<ArenaRound> practiceRounds = arenaRoundRepository
                .findByConditionNameAndDifficultyLevelAndPracticeTrueOrderByIdAsc(
                        session.getConditionName(), session.getDifficultyLevel());
        List<ArenaRound> served = practiceRounds.subList(0,
                Math.min(PRACTICE_ROUNDS_PER_SESSION, practiceRounds.size()));
        return roundShuffler.derivePracticeRounds(session.getShuffleSeed(), served);
    }

    private List<DerivedRound> derivedScoredRoundsForSession(GameSession session) {
        List<ArenaRound> rounds = arenaRoundRepository.findByConditionNameAndDifficultyLevelAndPracticeFalseOrderByIdAsc(
                session.getConditionName(),
                session.getDifficultyLevel()
        );
        return roundShuffler.deriveScoredRounds(session.getShuffleSeed(), rounds);
    }

    private DerivedRound derivedScoredRound(GameSession session, ArenaRound round) {
        for (DerivedRound derived : derivedScoredRoundsForSession(session)) {
            if (derived.getRound().getId().equals(round.getId())) {
                return derived;
            }
        }
        throw new BadRequestException("Round does not belong to this session condition and difficulty");
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

    private Ideophone getSelectedIdeophone(ArenaRound round, Long selectedIdeophoneId) {
        if (round.getLeftIdeophone().getId().equals(selectedIdeophoneId)) {
            return round.getLeftIdeophone();
        }
        if (round.getRightIdeophone().getId().equals(selectedIdeophoneId)) {
            return round.getRightIdeophone();
        }
        throw new BadRequestException("Selected ideophone is not an option for this round");
    }

    // Correctness is judged against the seed-derived target, never against
    // arena_rounds.correct_ideophone_id (which documents the thesis target).
    private boolean isCorrectChoice(DerivedRound derivedRound, Ideophone selectedIdeophone) {
        return derivedRound.getTarget().getId().equals(selectedIdeophone.getId());
    }
}
