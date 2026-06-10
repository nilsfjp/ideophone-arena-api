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
import io.github.nilsfjp.ideophonearena.model.GameSession;
import io.github.nilsfjp.ideophonearena.model.Ideophone;
import io.github.nilsfjp.ideophonearena.model.PlayerAnswer;
import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;
import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import io.github.nilsfjp.ideophonearena.repository.ArenaRoundRepository;
import io.github.nilsfjp.ideophonearena.repository.GameSessionRepository;
import io.github.nilsfjp.ideophonearena.repository.PlayerAnswerRepository;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {

    private static final int SUPPORTED_DIFFICULTY_LEVEL = 1;
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

    public GameService(AppUserRepository appUserRepository, GameSessionRepository gameSessionRepository,
            ArenaRoundRepository arenaRoundRepository, PlayerAnswerRepository playerAnswerRepository,
            GameMapper gameMapper) {
        this.appUserRepository = appUserRepository;
        this.gameSessionRepository = gameSessionRepository;
        this.arenaRoundRepository = arenaRoundRepository;
        this.playerAnswerRepository = playerAnswerRepository;
        this.gameMapper = gameMapper;
    }

    @Transactional
    public GameSessionResponse startSession(UserDetails userDetails, StartSessionRequest request) {
        AppUser user = getCurrentUser(userDetails);
        Integer difficultyLevel = request.getDifficultyLevel();
        ConditionName conditionName = request.getConditionName();

        validateSupportedStartRequest(conditionName, difficultyLevel);

        GameSession session = new GameSession(user, conditionName, difficultyLevel);
        return gameMapper.toSessionResponse(gameSessionRepository.save(session));
    }

    @Transactional
    public RoundResponse getNextRound(UserDetails userDetails, String sessionUuid) {
        AppUser user = getCurrentUser(userDetails);
        GameSession session = getOwnedSession(user, sessionUuid);

        List<ArenaRound> rounds = arenaRoundRepository.findByConditionNameAndDifficultyLevelOrderByIdAsc(
                session.getConditionName(),
                session.getDifficultyLevel()
        );
        if (rounds.isEmpty()) {
            throw new ResourceNotFoundException("No rounds found for this session");
        }

        Set<Long> answeredRoundIds = new HashSet<>(playerAnswerRepository.findAnsweredRoundIdsBySessionId(
                session.getId()));
        for (ArenaRound round : rounds) {
            if (!answeredRoundIds.contains(round.getId())) {
                return gameMapper.toRoundResponse(session, round);
            }
        }

        if (session.getCompletedAt() == null) {
            session.complete();
        }
        return RoundResponse.completed(
                session.getSessionUuid(),
                session.getConditionName(),
                session.getDifficultyLevel(),
                SESSION_COMPLETE_MESSAGE
        );
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
        if (playerAnswerRepository.existsBySessionIdAndRoundId(session.getId(), round.getId())) {
            throw new ConflictException("This round has already been answered in this session");
        }

        Ideophone selectedIdeophone = getSelectedIdeophone(round, request.getSelectedIdeophoneId());
        boolean correct = isCorrectChoice(round, selectedIdeophone);
        PlayerAnswer answer = new PlayerAnswer(session, round, selectedIdeophone, request.getResponseTimeMs(), correct);
        playerAnswerRepository.save(answer);

        long totalAnswered = playerAnswerRepository.countBySessionUserId(user.getId());
        long totalCorrect = playerAnswerRepository.countBySessionUserIdAndCorrectTrue(user.getId());

        return new AnswerResultResponse(
                round.getId(),
                selectedIdeophone.getId(),
                round.getCorrectIdeophone().getId(),
                answer.isCorrect(),
                round.getPrompt(),
                round.getCorrectIdeophone().getKana(),
                selectedIdeophone.getKana(),
                totalAnswered,
                totalCorrect
        );
    }

    private AppUser getCurrentUser(UserDetails userDetails) {
        return appUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private void validateSupportedStartRequest(ConditionName conditionName, Integer difficultyLevel) {
        if (difficultyLevel == null) {
            throw new BadRequestException("difficultyLevel is required");
        }
        if (difficultyLevel != SUPPORTED_DIFFICULTY_LEVEL) {
            throw new BadRequestException("Only difficulty level 1 is supported for the current demo");
        }
        if (conditionName == null) {
            throw new BadRequestException("conditionName is required");
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

    private boolean isCorrectChoice(ArenaRound round, Ideophone selectedIdeophone) {
        return round.getCorrectIdeophone().getId().equals(selectedIdeophone.getId());
    }
}
