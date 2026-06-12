package io.github.nilsfjp.ideophonearena.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.nilsfjp.ideophonearena.dto.AnswerResultResponse;
import io.github.nilsfjp.ideophonearena.dto.RoundResponse;
import io.github.nilsfjp.ideophonearena.dto.StartSessionRequest;
import io.github.nilsfjp.ideophonearena.dto.SubmitAnswerRequest;
import io.github.nilsfjp.ideophonearena.exception.BadRequestException;
import io.github.nilsfjp.ideophonearena.exception.ConflictException;
import io.github.nilsfjp.ideophonearena.mapper.GameMapper;
import io.github.nilsfjp.ideophonearena.model.AppUser;
import io.github.nilsfjp.ideophonearena.model.ArenaRound;
import io.github.nilsfjp.ideophonearena.model.DerivedRound;
import io.github.nilsfjp.ideophonearena.model.GameSession;
import io.github.nilsfjp.ideophonearena.model.Ideophone;
import io.github.nilsfjp.ideophonearena.model.PlayerAnswer;
import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import io.github.nilsfjp.ideophonearena.repository.ArenaRoundRepository;
import io.github.nilsfjp.ideophonearena.repository.GameSessionRepository;
import io.github.nilsfjp.ideophonearena.repository.PlayerAnswerRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class GameServiceTests {

    private static final String USERNAME = "player";
    private static final String SESSION_UUID = "session-uuid";

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private GameSessionRepository gameSessionRepository;

    @Mock
    private ArenaRoundRepository arenaRoundRepository;

    @Mock
    private PlayerAnswerRepository playerAnswerRepository;

    @Mock
    private UserDetails userDetails;

    private final RoundShuffler roundShuffler = new RoundShuffler();
    private GameService gameService;
    private AppUser user;
    private GameSession session;

    @BeforeEach
    void setUp() {
        gameService = new GameService(
                appUserRepository,
                gameSessionRepository,
                arenaRoundRepository,
                playerAnswerRepository,
                new GameMapper(),
                roundShuffler
        );
        user = new AppUser(USERNAME, "player@example.test", "hash");
        setId(user, 10L);
        session = new GameSession(user, ConditionName.CONDITION_1_SOKUON, 1);
        setId(session, 20L);
        session.setSessionUuid(SESSION_UUID);

        when(userDetails.getUsername()).thenReturn(USERNAME);
        when(appUserRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
    }

    @Test
    void startSessionRejectsUnsupportedDifficulty() {
        StartSessionRequest request = new StartSessionRequest();
        request.setConditionName(ConditionName.CONDITION_1_SOKUON);
        request.setDifficultyLevel(2);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> gameService.startSession(userDetails, request)
        );

        assertEquals("Only difficulty level 1 is supported for the current demo", exception.getMessage());
        verify(gameSessionRepository, never()).save(org.mockito.ArgumentMatchers.any(GameSession.class));
    }

    @Test
    void startSessionRejectsUnsupportedCondition() {
        StartSessionRequest request = new StartSessionRequest();
        request.setConditionName(ConditionName.TEXT_ONLY);
        request.setDifficultyLevel(1);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> gameService.startSession(userDetails, request)
        );

        assertEquals(
                "Unsupported conditionName: TEXT_ONLY. Supported values are CONDITION_1_SOKUON, CONDITION_2_SOKUON, CONDITION_3_SOKUON",
                exception.getMessage()
        );
        verify(gameSessionRepository, never()).save(org.mockito.ArgumentMatchers.any(GameSession.class));
    }

    @Test
    void startSessionCreatesSupportedConditionWithoutDefaulting() {
        StartSessionRequest request = new StartSessionRequest();
        request.setConditionName(ConditionName.CONDITION_2_SOKUON);
        request.setDifficultyLevel(1);
        when(gameSessionRepository.save(org.mockito.ArgumentMatchers.any(GameSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        gameService.startSession(userDetails, request);

        ArgumentCaptor<GameSession> sessionCaptor = ArgumentCaptor.forClass(GameSession.class);
        verify(gameSessionRepository).save(sessionCaptor.capture());
        GameSession savedSession = sessionCaptor.getValue();
        assertEquals(ConditionName.CONDITION_2_SOKUON, savedSession.getConditionName());
        assertEquals(1, savedSession.getDifficultyLevel());
    }

    @Test
    void getNextRoundReturnsFirstUnansweredRoundForSession() {
        ArenaRound answeredRound = round(
                100L,
                "with a rustling sound",
                ideophone(1L, "ごそごそ", "gosogoso", "with a rustling sound", "a0hu-gosogoso.mp4"),
                ideophone(2L, "かたかた", "katakata", "clattering, rattling", "a0kd-katakata.mp4")
        );
        ArenaRound nextRound = round(
                101L,
                "drizzling",
                ideophone(3L, "しとしと", "sitosito", "drizzling", "a1hu-sitosito.mp4"),
                ideophone(4L, "ばちゃばちゃ", "batyabatya", "splashing", "a1kd-batyabatya.mp4")
        );
        when(gameSessionRepository.findBySessionUuid(SESSION_UUID)).thenReturn(Optional.of(session));
        when(arenaRoundRepository.findByConditionNameAndDifficultyLevelAndPracticeFalseOrderByIdAsc(
                ConditionName.CONDITION_1_SOKUON,
                1
        )).thenReturn(List.of(answeredRound, nextRound));
        when(playerAnswerRepository.findAnsweredRoundIdsBySessionId(20L)).thenReturn(List.of(100L));

        RoundResponse response = gameService.getNextRound(userDetails, SESSION_UUID);

        DerivedRound expected = derivedScoredRound(List.of(answeredRound, nextRound), 101L);
        assertEquals(101L, response.getRoundId());
        assertEquals(expected.getTarget().getGloss(), response.getTargetTranslation());
        assertEquals(expected.getTarget().getGloss(), response.getPrompt());
        assertEquals(expected.getTarget().getGloss(), response.getTranslations().getTarget());
        assertEquals(expected.getOther().getGloss(), response.getTranslations().getOther());
        assertEquals(expected.getLeft().getId(), response.getLeft().getIdeophoneId());
        assertEquals(expected.getRight().getId(), response.getRight().getIdeophoneId());
        assertEquals("/stimuli/" + expected.getLeft().getStimulusFile(), response.getLeft().getStimulusUrl());
        assertEquals(800, response.getTiming().getFixationMs());
    }

    @Test
    void getNextRoundResumesIdenticallyAfterServiceRestart() {
        ArenaRound firstRound = round(
                100L,
                "with a rustling sound",
                ideophone(1L, "ごそごそ", "gosogoso", "with a rustling sound", "a0hu-gosogoso.mp4"),
                ideophone(2L, "かたかた", "katakata", "clattering, rattling", "a0kd-katakata.mp4")
        );
        ArenaRound secondRound = round(
                101L,
                "drizzling",
                ideophone(3L, "しとしと", "sitosito", "drizzling", "a1hu-sitosito.mp4"),
                ideophone(4L, "ばちゃばちゃ", "batyabatya", "splashing", "a1kd-batyabatya.mp4")
        );
        ArenaRound thirdRound = round(
                102L,
                "noisily gushing",
                ideophone(5L, "じゃあじゃあ", "zyaazyaa", "noisily gushing", "a2hu-zyaazyaa.mp4"),
                ideophone(6L, "ぽたぽた", "potapota", "dripping, trickling", "a2kd-potapota.mp4")
        );
        session.setShuffleSeed(424242L);
        List<ArenaRound> rounds = List.of(firstRound, secondRound, thirdRound);
        List<DerivedRound> derivedOrder = roundShuffler.deriveScoredRounds(424242L, rounds);
        List<Long> answeredFirstTwo = List.of(
                derivedOrder.get(0).getRound().getId(),
                derivedOrder.get(1).getRound().getId()
        );
        when(gameSessionRepository.findBySessionUuid(SESSION_UUID)).thenReturn(Optional.of(session));
        when(arenaRoundRepository.findByConditionNameAndDifficultyLevelAndPracticeFalseOrderByIdAsc(
                ConditionName.CONDITION_1_SOKUON,
                1
        )).thenReturn(rounds);
        when(playerAnswerRepository.findAnsweredRoundIdsBySessionId(20L)).thenReturn(answeredFirstTwo);

        // A fresh service and shuffler stand in for a restarted server: the
        // derivation must continue exactly where the session left off.
        GameService restartedService = new GameService(
                appUserRepository,
                gameSessionRepository,
                arenaRoundRepository,
                playerAnswerRepository,
                new GameMapper(),
                new RoundShuffler()
        );

        RoundResponse response = restartedService.getNextRound(userDetails, SESSION_UUID);

        DerivedRound expected = derivedOrder.get(2);
        assertEquals(expected.getRound().getId(), response.getRoundId());
        assertEquals(expected.getTarget().getGloss(), response.getTargetTranslation());
        assertEquals(expected.getLeft().getId(), response.getLeft().getIdeophoneId());
        assertEquals(expected.getRight().getId(), response.getRight().getIdeophoneId());
    }

    @Test
    void getNextRoundReturnsCompletionResponseAfterAllRoundsAreAnswered() {
        ArenaRound answeredRound = round(
                100L,
                "with a rustling sound",
                ideophone(1L, "ごそごそ", "gosogoso", "with a rustling sound", "a0hu-gosogoso.mp4"),
                ideophone(2L, "かたかた", "katakata", "clattering, rattling", "a0kd-katakata.mp4")
        );
        when(gameSessionRepository.findBySessionUuid(SESSION_UUID)).thenReturn(Optional.of(session));
        when(arenaRoundRepository.findByConditionNameAndDifficultyLevelAndPracticeFalseOrderByIdAsc(
                ConditionName.CONDITION_1_SOKUON,
                1
        )).thenReturn(List.of(answeredRound));
        when(playerAnswerRepository.findAnsweredRoundIdsBySessionId(20L)).thenReturn(List.of(100L));

        RoundResponse response = gameService.getNextRound(userDetails, SESSION_UUID);

        assertTrue(response.isCompleted());
        assertEquals("Game session is complete", response.getMessage());
        assertEquals(SESSION_UUID, response.getSessionUuid());
        assertEquals(ConditionName.CONDITION_1_SOKUON, response.getConditionName());
        assertEquals(1, response.getDifficultyLevel());
        assertNull(session.getCompletedAt());
    }

    @Test
    void submitAnswerJudgesAgainstDerivedTargetAndStoresIt() {
        Ideophone left = ideophone(1L, "ごそごそ", "gosogoso", "with a rustling sound", "a0hu-gosogoso.mp4");
        Ideophone right = ideophone(2L, "かたかた", "katakata", "clattering, rattling", "a0kd-katakata.mp4");
        ArenaRound round = round(100L, "with a rustling sound", left, right);
        DerivedRound derived = derivedScoredRound(List.of(round), 100L);
        SubmitAnswerRequest request = new SubmitAnswerRequest();
        request.setRoundId(100L);
        request.setSelectedIdeophoneId(derived.getTarget().getId());
        request.setResponseTimeMs(1234);
        when(gameSessionRepository.findBySessionUuid(SESSION_UUID)).thenReturn(Optional.of(session));
        when(arenaRoundRepository.findByIdWithIdeophones(100L)).thenReturn(Optional.of(round));
        when(playerAnswerRepository.existsBySessionIdAndRoundId(20L, 100L)).thenReturn(false);
        stubScoredRounds(List.of(round));
        when(playerAnswerRepository.countBySessionId(20L)).thenReturn(1L);
        when(playerAnswerRepository.countBySessionIdAndCorrectTrue(20L)).thenReturn(1L);
        when(arenaRoundRepository.countByConditionNameAndDifficultyLevelAndPracticeFalse(
                ConditionName.CONDITION_1_SOKUON, 1)).thenReturn(60L);

        AnswerResultResponse response = gameService.submitAnswer(userDetails, SESSION_UUID, request);

        ArgumentCaptor<PlayerAnswer> answerCaptor = ArgumentCaptor.forClass(PlayerAnswer.class);
        verify(playerAnswerRepository).saveAndFlush(answerCaptor.capture());
        PlayerAnswer savedAnswer = answerCaptor.getValue();
        assertEquals(session, savedAnswer.getSession());
        assertEquals(round, savedAnswer.getRound());
        assertEquals(derived.getTarget(), savedAnswer.getSelectedIdeophone());
        assertEquals(derived.getTarget(), savedAnswer.getTargetIdeophone());
        assertEquals(1234, savedAnswer.getResponseTimeMs());
        assertTrue(savedAnswer.isCorrect());
        assertEquals(100L, response.getRoundId());
        assertEquals(derived.getTarget().getId(), response.getSelectedIdeophoneId());
        assertEquals(derived.getTarget().getId(), response.getCorrectIdeophoneId());
        assertTrue(response.isCorrect());
        assertEquals(derived.getTarget().getGloss(), response.getTargetTranslation());
        assertEquals(derived.getTarget().getKana(), response.getSelectedKana());
        assertEquals(derived.getTarget().getKana(), response.getCorrectKana());
        assertEquals(1L, response.getTotalAnswered());
        assertEquals(1L, response.getTotalCorrect());
        assertNull(session.getCompletedAt());
    }

    @Test
    void submitAnswerMarksDerivedDistractorIncorrectAndStillStoresTarget() {
        Ideophone left = ideophone(1L, "ごそごそ", "gosogoso", "with a rustling sound", "a0hu-gosogoso.mp4");
        Ideophone right = ideophone(2L, "かたかた", "katakata", "clattering, rattling", "a0kd-katakata.mp4");
        ArenaRound round = round(100L, "with a rustling sound", left, right);
        DerivedRound derived = derivedScoredRound(List.of(round), 100L);
        SubmitAnswerRequest request = new SubmitAnswerRequest();
        request.setRoundId(100L);
        request.setSelectedIdeophoneId(derived.getOther().getId());
        request.setResponseTimeMs(1234);
        when(gameSessionRepository.findBySessionUuid(SESSION_UUID)).thenReturn(Optional.of(session));
        when(arenaRoundRepository.findByIdWithIdeophones(100L)).thenReturn(Optional.of(round));
        when(playerAnswerRepository.existsBySessionIdAndRoundId(20L, 100L)).thenReturn(false);
        stubScoredRounds(List.of(round));
        when(playerAnswerRepository.countBySessionId(20L)).thenReturn(1L);
        when(playerAnswerRepository.countBySessionIdAndCorrectTrue(20L)).thenReturn(0L);
        when(arenaRoundRepository.countByConditionNameAndDifficultyLevelAndPracticeFalse(
                ConditionName.CONDITION_1_SOKUON, 1)).thenReturn(60L);

        AnswerResultResponse response = gameService.submitAnswer(userDetails, SESSION_UUID, request);

        ArgumentCaptor<PlayerAnswer> answerCaptor = ArgumentCaptor.forClass(PlayerAnswer.class);
        verify(playerAnswerRepository).saveAndFlush(answerCaptor.capture());
        PlayerAnswer savedAnswer = answerCaptor.getValue();
        assertEquals(derived.getOther(), savedAnswer.getSelectedIdeophone());
        assertEquals(derived.getTarget(), savedAnswer.getTargetIdeophone());
        assertFalse(savedAnswer.isCorrect());
        assertFalse(response.isCorrect());
        assertEquals(derived.getTarget().getId(), response.getCorrectIdeophoneId());
    }

    @Test
    void submitAnswerMarksSessionCompleteWhenLastRoundIsAnswered() {
        Ideophone left = ideophone(1L, "ごそごそ", "gosogoso", "with a rustling sound", "a0hu-gosogoso.mp4");
        Ideophone right = ideophone(2L, "かたかた", "katakata", "clattering, rattling", "a0kd-katakata.mp4");
        ArenaRound round = round(100L, "with a rustling sound", left, right);
        SubmitAnswerRequest request = new SubmitAnswerRequest();
        request.setRoundId(100L);
        request.setSelectedIdeophoneId(1L);
        request.setResponseTimeMs(1234);
        when(gameSessionRepository.findBySessionUuid(SESSION_UUID)).thenReturn(Optional.of(session));
        when(arenaRoundRepository.findByIdWithIdeophones(100L)).thenReturn(Optional.of(round));
        when(playerAnswerRepository.existsBySessionIdAndRoundId(20L, 100L)).thenReturn(false);
        stubScoredRounds(List.of(round));
        when(playerAnswerRepository.countBySessionId(20L)).thenReturn(60L);
        when(playerAnswerRepository.countBySessionIdAndCorrectTrue(20L)).thenReturn(45L);
        when(arenaRoundRepository.countByConditionNameAndDifficultyLevelAndPracticeFalse(
                ConditionName.CONDITION_1_SOKUON, 1)).thenReturn(60L);

        AnswerResultResponse response = gameService.submitAnswer(userDetails, SESSION_UUID, request);

        assertEquals(60L, response.getTotalAnswered());
        assertEquals(45L, response.getTotalCorrect());
        assertTrue(session.getCompletedAt() != null);
    }

    @Test
    void submitAnswerTranslatesConcurrentDuplicateInsertToConflict() {
        Ideophone left = ideophone(1L, "ごそごそ", "gosogoso", "with a rustling sound", "a0hu-gosogoso.mp4");
        Ideophone right = ideophone(2L, "かたかた", "katakata", "clattering, rattling", "a0kd-katakata.mp4");
        ArenaRound round = round(100L, "with a rustling sound", left, right);
        SubmitAnswerRequest request = new SubmitAnswerRequest();
        request.setRoundId(100L);
        request.setSelectedIdeophoneId(1L);
        request.setResponseTimeMs(1234);
        when(gameSessionRepository.findBySessionUuid(SESSION_UUID)).thenReturn(Optional.of(session));
        when(arenaRoundRepository.findByIdWithIdeophones(100L)).thenReturn(Optional.of(round));
        when(playerAnswerRepository.existsBySessionIdAndRoundId(20L, 100L)).thenReturn(false);
        stubScoredRounds(List.of(round));
        when(playerAnswerRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(PlayerAnswer.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(ConflictException.class, () -> gameService.submitAnswer(userDetails, SESSION_UUID, request));
        assertNull(session.getCompletedAt());
    }

    @Test
    void submitAnswerRejectsIdeophoneThatIsNotAChoiceForTheRound() {
        ArenaRound round = round(
                100L,
                "with a rustling sound",
                ideophone(1L, "ごそごそ", "gosogoso", "with a rustling sound", "a0hu-gosogoso.mp4"),
                ideophone(2L, "かたかた", "katakata", "clattering, rattling", "a0kd-katakata.mp4")
        );
        SubmitAnswerRequest request = new SubmitAnswerRequest();
        request.setRoundId(100L);
        request.setSelectedIdeophoneId(999L);
        request.setResponseTimeMs(500);
        when(gameSessionRepository.findBySessionUuid(SESSION_UUID)).thenReturn(Optional.of(session));
        when(arenaRoundRepository.findByIdWithIdeophones(100L)).thenReturn(Optional.of(round));
        when(playerAnswerRepository.existsBySessionIdAndRoundId(20L, 100L)).thenReturn(false);
        stubScoredRounds(List.of(round));

        assertThrows(BadRequestException.class, () -> gameService.submitAnswer(userDetails, SESSION_UUID, request));
        verify(playerAnswerRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any(PlayerAnswer.class));
    }

    @Test
    void getNextRoundServesPracticeRoundsBeforeScoredRounds() {
        session.setIncludePractice(true);
        ArenaRound firstPractice = practiceRound(
                900L,
                "softly, gently",
                ideophone(31L, "そっと", "sotto", "softly, gently", "audio/p0h-sotto.m4a"),
                ideophone(32L, "がたん", "gataN", "with a bang", "audio/p0k-gataN.m4a")
        );
        ArenaRound secondPractice = practiceRound(
                901L,
                "suddenly, in a flash",
                ideophone(33L, "じっと", "zitto", "motionless, fixedly", "audio/p1h-zitto.m4a"),
                ideophone(34L, "ぱっ", "paQ", "suddenly, in a flash", "audio/p1k-paQ.m4a")
        );
        when(gameSessionRepository.findBySessionUuid(SESSION_UUID)).thenReturn(Optional.of(session));
        when(arenaRoundRepository.findByConditionNameAndDifficultyLevelAndPracticeTrueOrderByIdAsc(
                ConditionName.CONDITION_1_SOKUON,
                1
        )).thenReturn(List.of(firstPractice, secondPractice));

        RoundResponse response = gameService.getNextRound(userDetails, SESSION_UUID);

        assertEquals(900L, response.getRoundId());
        assertTrue(response.isPractice());
        verify(arenaRoundRepository, never()).findByConditionNameAndDifficultyLevelAndPracticeFalseOrderByIdAsc(
                ConditionName.CONDITION_1_SOKUON, 1);
    }

    @Test
    void submitPracticeAnswerReturnsFeedbackWithoutPersistingAnswer() {
        session.setIncludePractice(true);
        Ideophone left = ideophone(31L, "そっと", "sotto", "softly, gently", "audio/p0h-sotto.m4a");
        Ideophone right = ideophone(32L, "がたん", "gataN", "with a bang", "audio/p0k-gataN.m4a");
        ArenaRound practiceRound = practiceRound(900L, "softly, gently", left, right);
        DerivedRound derived = roundShuffler
                .derivePracticeRounds(session.getShuffleSeed(), List.of(practiceRound))
                .get(0);
        SubmitAnswerRequest request = new SubmitAnswerRequest();
        request.setRoundId(900L);
        request.setSelectedIdeophoneId(derived.getTarget().getId());
        request.setResponseTimeMs(1234);
        when(gameSessionRepository.findBySessionUuid(SESSION_UUID)).thenReturn(Optional.of(session));
        when(arenaRoundRepository.findByIdWithIdeophones(900L)).thenReturn(Optional.of(practiceRound));
        when(arenaRoundRepository.findByConditionNameAndDifficultyLevelAndPracticeTrueOrderByIdAsc(
                ConditionName.CONDITION_1_SOKUON,
                1
        )).thenReturn(List.of(practiceRound));
        when(playerAnswerRepository.countBySessionId(20L)).thenReturn(0L);
        when(playerAnswerRepository.countBySessionIdAndCorrectTrue(20L)).thenReturn(0L);

        AnswerResultResponse response = gameService.submitAnswer(userDetails, SESSION_UUID, request);

        assertTrue(response.isPractice());
        assertTrue(response.isCorrect());
        assertEquals(0L, response.getTotalAnswered());
        assertEquals(0L, response.getTotalCorrect());
        assertEquals(1, session.getPracticeAnswered());
        assertNull(session.getCompletedAt());
        verify(playerAnswerRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any(PlayerAnswer.class));
    }

    @Test
    void submitPracticeAnswerRejectsOutOfOrderAndRepeatedRounds() {
        session.setIncludePractice(true);
        ArenaRound firstPractice = practiceRound(
                900L,
                "softly, gently",
                ideophone(31L, "そっと", "sotto", "softly, gently", "audio/p0h-sotto.m4a"),
                ideophone(32L, "がたん", "gataN", "with a bang", "audio/p0k-gataN.m4a")
        );
        ArenaRound secondPractice = practiceRound(
                901L,
                "suddenly, in a flash",
                ideophone(33L, "じっと", "zitto", "motionless, fixedly", "audio/p1h-zitto.m4a"),
                ideophone(34L, "ぱっ", "paQ", "suddenly, in a flash", "audio/p1k-paQ.m4a")
        );
        when(gameSessionRepository.findBySessionUuid(SESSION_UUID)).thenReturn(Optional.of(session));
        when(arenaRoundRepository.findByIdWithIdeophones(901L)).thenReturn(Optional.of(secondPractice));
        when(arenaRoundRepository.findByConditionNameAndDifficultyLevelAndPracticeTrueOrderByIdAsc(
                ConditionName.CONDITION_1_SOKUON,
                1
        )).thenReturn(List.of(firstPractice, secondPractice));

        SubmitAnswerRequest outOfOrder = new SubmitAnswerRequest();
        outOfOrder.setRoundId(901L);
        outOfOrder.setSelectedIdeophoneId(33L);
        outOfOrder.setResponseTimeMs(500);
        assertThrows(BadRequestException.class,
                () -> gameService.submitAnswer(userDetails, SESSION_UUID, outOfOrder));

        session.setPracticeAnswered(2);
        SubmitAnswerRequest repeated = new SubmitAnswerRequest();
        repeated.setRoundId(901L);
        repeated.setSelectedIdeophoneId(33L);
        repeated.setResponseTimeMs(500);
        assertThrows(ConflictException.class,
                () -> gameService.submitAnswer(userDetails, SESSION_UUID, repeated));
        verify(playerAnswerRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any(PlayerAnswer.class));
    }

    private void stubScoredRounds(List<ArenaRound> rounds) {
        when(arenaRoundRepository.findByConditionNameAndDifficultyLevelAndPracticeFalseOrderByIdAsc(
                ConditionName.CONDITION_1_SOKUON,
                1
        )).thenReturn(rounds);
    }

    private DerivedRound derivedScoredRound(List<ArenaRound> rounds, Long roundId) {
        return roundShuffler.deriveScoredRounds(session.getShuffleSeed(), rounds).stream()
                .filter(derived -> derived.getRound().getId().equals(roundId))
                .findFirst()
                .orElseThrow();
    }

    private ArenaRound round(Long id, String prompt, Ideophone left, Ideophone right) {
        ArenaRound round = new ArenaRound(
                prompt,
                left,
                right,
                left,
                ConditionName.CONDITION_1_SOKUON,
                1
        );
        setId(round, id);
        return round;
    }

    private ArenaRound practiceRound(Long id, String prompt, Ideophone left, Ideophone right) {
        ArenaRound round = new ArenaRound(
                prompt,
                left,
                right,
                left,
                ConditionName.CONDITION_1_SOKUON,
                1,
                true
        );
        setId(round, id);
        return round;
    }

    private Ideophone ideophone(Long id, String kana, String romaji, String gloss, String stimulusFile) {
        Ideophone ideophone = new Ideophone(kana, kana, kana, romaji, gloss, "HU", stimulusFile, Modality.AUDITORY);
        setId(ideophone, id);
        return ideophone;
    }

    private void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new IllegalStateException("Could not set test id", exception);
        }
    }
}
