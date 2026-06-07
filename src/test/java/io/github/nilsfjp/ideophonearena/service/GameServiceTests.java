package io.github.nilsfjp.ideophonearena.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import io.github.nilsfjp.ideophonearena.mapper.GameMapper;
import io.github.nilsfjp.ideophonearena.model.AppUser;
import io.github.nilsfjp.ideophonearena.model.ArenaRound;
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
                new GameMapper()
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
        when(arenaRoundRepository.findByConditionNameAndDifficultyLevelOrderByIdAsc(
                ConditionName.CONDITION_1_SOKUON,
                1
        )).thenReturn(List.of(answeredRound, nextRound));
        when(playerAnswerRepository.findAnsweredRoundIdsBySessionId(20L)).thenReturn(List.of(100L));

        RoundResponse response = gameService.getNextRound(userDetails, SESSION_UUID);

        assertEquals(101L, response.getRoundId());
        assertEquals("drizzling", response.getTargetTranslation());
        assertEquals("drizzling", response.getPrompt());
        assertEquals("drizzling", response.getTranslations().getTarget());
        assertEquals("splashing", response.getTranslations().getOther());
        assertEquals(3L, response.getLeft().getIdeophoneId());
        assertEquals("/stimuli/a1hu-sitosito.mp4", response.getLeft().getStimulusUrl());
        assertEquals(800, response.getTiming().getFixationMs());
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
        when(arenaRoundRepository.findByConditionNameAndDifficultyLevelOrderByIdAsc(
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
        assertTrue(session.getCompletedAt() != null);
    }

    @Test
    void submitAnswerPersistsSelectedIdeophoneAndReturnsFeedback() {
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
        when(playerAnswerRepository.countBySessionUserId(10L)).thenReturn(1L);
        when(playerAnswerRepository.countBySessionUserIdAndCorrectTrue(10L)).thenReturn(1L);

        AnswerResultResponse response = gameService.submitAnswer(userDetails, SESSION_UUID, request);

        ArgumentCaptor<PlayerAnswer> answerCaptor = ArgumentCaptor.forClass(PlayerAnswer.class);
        verify(playerAnswerRepository).save(answerCaptor.capture());
        PlayerAnswer savedAnswer = answerCaptor.getValue();
        assertEquals(session, savedAnswer.getSession());
        assertEquals(round, savedAnswer.getRound());
        assertEquals(left, savedAnswer.getSelectedIdeophone());
        assertEquals(1234, savedAnswer.getResponseTimeMs());
        assertTrue(savedAnswer.isCorrect());
        assertEquals(100L, response.getRoundId());
        assertEquals(1L, response.getSelectedIdeophoneId());
        assertEquals(1L, response.getCorrectIdeophoneId());
        assertTrue(response.isCorrect());
        assertEquals("with a rustling sound", response.getTargetTranslation());
        assertEquals("ごそごそ", response.getSelectedKana());
        assertEquals("ごそごそ", response.getCorrectKana());
        assertEquals(1L, response.getTotalAnswered());
        assertEquals(1L, response.getTotalCorrect());
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

        assertThrows(BadRequestException.class, () -> gameService.submitAnswer(userDetails, SESSION_UUID, request));
        verify(playerAnswerRepository, never()).save(org.mockito.ArgumentMatchers.any(PlayerAnswer.class));
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

    private Ideophone ideophone(Long id, String kana, String romaji, String gloss, String stimulusFile) {
        Ideophone ideophone = new Ideophone(kana, romaji, gloss, "HU", stimulusFile, Modality.AUDITORY);
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
