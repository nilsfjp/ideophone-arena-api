package io.github.nilsfjp.ideophonearena.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.nilsfjp.ideophonearena.dto.AnswerResultResponse;
import io.github.nilsfjp.ideophonearena.dto.GameSessionResponse;
import io.github.nilsfjp.ideophonearena.dto.RoundResponse;
import io.github.nilsfjp.ideophonearena.dto.StartSessionRequest;
import io.github.nilsfjp.ideophonearena.dto.SubmitAnswerRequest;
import io.github.nilsfjp.ideophonearena.exception.BadRequestException;
import io.github.nilsfjp.ideophonearena.exception.ConflictException;
import io.github.nilsfjp.ideophonearena.mapper.GameMapper;
import io.github.nilsfjp.ideophonearena.model.AppUser;
import io.github.nilsfjp.ideophonearena.model.DerivedRound;
import io.github.nilsfjp.ideophonearena.model.GameSession;
import io.github.nilsfjp.ideophonearena.model.Pairing;
import io.github.nilsfjp.ideophonearena.model.PlayerAnswer;
import io.github.nilsfjp.ideophonearena.model.Presentation;
import io.github.nilsfjp.ideophonearena.model.Trial;
import io.github.nilsfjp.ideophonearena.model.Word;
import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;
import io.github.nilsfjp.ideophonearena.model.enums.GameMode;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import io.github.nilsfjp.ideophonearena.repository.GameSessionRepository;
import io.github.nilsfjp.ideophonearena.repository.PlayerAnswerRepository;
import io.github.nilsfjp.ideophonearena.repository.PresentationRepository;
import io.github.nilsfjp.ideophonearena.repository.TrialRepository;
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
    private TrialRepository trialRepository;

    @Mock
    private PresentationRepository presentationRepository;

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
                trialRepository,
                presentationRepository,
                playerAnswerRepository,
                new GameMapper(),
                roundShuffler,
                new LadderFloors(),
                roundSources()
        );
        user = new AppUser(USERNAME, "player@example.test", "hash");
        setId(user, 10L);
        session = new GameSession(user, ConditionName.CONDITION_1_SOKUON, false, 0L);
        setId(session, 20L);
        session.setSessionUuid(SESSION_UUID);

        when(userDetails.getUsername()).thenReturn(USERNAME);
        when(appUserRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
    }

    private List<RoundSource> roundSources() {
        return List.of(
                new ChoosingRoundSource(trialRepository, roundShuffler),
                new LadderRoundSource(trialRepository, roundShuffler, new LadderFloors())
        );
    }

    @Test
    void startSessionRejectsUnsupportedCondition() {
        // All ConditionName values are supported after the TEXT_ONLY cut (A1), so a
        // service-level "unsupported condition" is only reachable if the allowlist and the
        // enum ever diverge; the unknown-string case is an HTTP deserialization concern
        // (see GameLoopHttpTests). Here we assert the ladder-mode guardrails instead.
        StartSessionRequest request = new StartSessionRequest();
        request.setConditionName(ConditionName.CONDITION_1_SOKUON);
        request.setGameMode(GameMode.LADDER);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> gameService.startSession(userDetails, request)
        );

        assertEquals("A ladder session requires a floor", exception.getMessage());
        verify(gameSessionRepository, never()).save(any(GameSession.class));
    }

    @Test
    void startSessionRejectsFloorForChoosingMode() {
        StartSessionRequest request = new StartSessionRequest();
        request.setConditionName(ConditionName.CONDITION_1_SOKUON);
        request.setFloor(Modality.AUDITORY);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> gameService.startSession(userDetails, request)
        );

        assertEquals("A floor is only valid for a ladder session", exception.getMessage());
        verify(gameSessionRepository, never()).save(any(GameSession.class));
    }

    @Test
    void startSessionRejectsLadderWithPractice() {
        StartSessionRequest request = new StartSessionRequest();
        request.setConditionName(ConditionName.CONDITION_1_SOKUON);
        request.setGameMode(GameMode.LADDER);
        request.setFloor(Modality.AUDITORY);
        request.setIncludePractice(true);
        when(trialRepository.findScoredTrialsByPairCodes(any())).thenReturn(List.of(
                trial(1L, word(1L, "a", "a", "a", "audio/a9h-a.m4a"), word(2L, "b", "b", "b", "audio/a9k-b.m4a"))));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> gameService.startSession(userDetails, request)
        );

        assertEquals("Ladder sessions do not include practice rounds", exception.getMessage());
        verify(gameSessionRepository, never()).save(any(GameSession.class));
    }

    @Test
    void startSessionCreatesSupportedConditionWithoutDefaulting() {
        StartSessionRequest request = new StartSessionRequest();
        request.setConditionName(ConditionName.CONDITION_2_SOKUON);
        when(gameSessionRepository.save(any(GameSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        gameService.startSession(userDetails, request);

        ArgumentCaptor<GameSession> sessionCaptor = ArgumentCaptor.forClass(GameSession.class);
        verify(gameSessionRepository).save(sessionCaptor.capture());
        GameSession savedSession = sessionCaptor.getValue();
        assertEquals(ConditionName.CONDITION_2_SOKUON, savedSession.getConditionName());
        assertEquals(GameMode.CHOOSING, savedSession.getGameMode());
        assertNull(savedSession.getLadderFloor());
        // difficulty is the locked invariant (A3): never client-supplied, always 1.
        assertEquals(1, savedSession.getDifficultyLevel());
    }

    @Test
    void startSessionCreatesLadderSessionForServedFloor() {
        StartSessionRequest request = new StartSessionRequest();
        request.setConditionName(ConditionName.CONDITION_1_SOKUON);
        request.setGameMode(GameMode.LADDER);
        request.setFloor(Modality.AUDITORY);
        // LadderRoundSource keys trials by the floor's pair codes, so the served trial must
        // carry one of them -- an arbitrary code resolves to an empty floor.
        String servedPairCode = new LadderFloors().pairCodesInOrder(Modality.AUDITORY).get(0);
        when(trialRepository.findScoredTrialsByPairCodes(any())).thenReturn(List.of(
                ladderTrial(1L, servedPairCode,
                        word(1L, "a", "a", "a", "audio/a9h-a.m4a"), word(2L, "b", "b", "b", "audio/a9k-b.m4a"))));
        when(gameSessionRepository.save(any(GameSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GameSessionResponse response = gameService.startSession(userDetails, request);

        ArgumentCaptor<GameSession> sessionCaptor = ArgumentCaptor.forClass(GameSession.class);
        verify(gameSessionRepository).save(sessionCaptor.capture());
        GameSession savedSession = sessionCaptor.getValue();
        assertEquals(GameMode.LADDER, savedSession.getGameMode());
        assertEquals(Modality.AUDITORY, savedSession.getLadderFloor());
        // totalRounds comes off the RoundSource seam, so it tracks the mode's scored rounds
        // -- a ladder floor's pair count, never the CHOOSING pool.
        assertEquals(1, response.getTotalRounds());
    }

    @Test
    void getNextRoundReturnsFirstUnansweredRoundForSession() {
        Trial answeredTrial = trial(
                100L,
                word(1L, "ごそごそ", "gosogoso", "with a rustling sound", "audio/a0h-gosogoso.m4a"),
                word(2L, "かたかた", "katakata", "clattering, rattling", "audio/a0k-katakata.m4a")
        );
        Trial nextTrial = trial(
                101L,
                word(3L, "しとしと", "sitosito", "drizzling", "audio/a1h-sitosito.m4a"),
                word(4L, "ばちゃばちゃ", "batyabatya", "splashing", "audio/a1k-batyabatya.m4a")
        );
        when(gameSessionRepository.findBySessionUuid(SESSION_UUID)).thenReturn(Optional.of(session));
        when(trialRepository.findScoredChoosingTrials()).thenReturn(List.of(answeredTrial, nextTrial));
        when(playerAnswerRepository.findAnsweredTrialIdsBySessionId(20L)).thenReturn(List.of(100L));
        stubPresentations(nextTrial);

        RoundResponse response = gameService.getNextRound(userDetails, SESSION_UUID);

        DerivedRound expected = derivedScoredRound(List.of(answeredTrial, nextTrial), 101L);
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
        Trial firstTrial = trial(
                100L,
                word(1L, "ごそごそ", "gosogoso", "with a rustling sound", "audio/a0h-gosogoso.m4a"),
                word(2L, "かたかた", "katakata", "clattering, rattling", "audio/a0k-katakata.m4a")
        );
        Trial secondTrial = trial(
                101L,
                word(3L, "しとしと", "sitosito", "drizzling", "audio/a1h-sitosito.m4a"),
                word(4L, "ばちゃばちゃ", "batyabatya", "splashing", "audio/a1k-batyabatya.m4a")
        );
        Trial thirdTrial = trial(
                102L,
                word(5L, "じゃあじゃあ", "zyaazyaa", "noisily gushing", "audio/a2h-zyaazyaa.m4a"),
                word(6L, "ぽたぽた", "potapota", "dripping, trickling", "audio/a2k-potapota.m4a")
        );
        session.setShuffleSeed(424242L);
        List<Trial> trials = List.of(firstTrial, secondTrial, thirdTrial);
        List<DerivedRound> derivedOrder = roundShuffler.deriveScoredRounds(424242L, trials);
        List<Long> answeredFirstTwo = List.of(
                derivedOrder.get(0).getTrial().getId(),
                derivedOrder.get(1).getTrial().getId()
        );
        when(gameSessionRepository.findBySessionUuid(SESSION_UUID)).thenReturn(Optional.of(session));
        when(trialRepository.findScoredChoosingTrials()).thenReturn(trials);
        when(playerAnswerRepository.findAnsweredTrialIdsBySessionId(20L)).thenReturn(answeredFirstTwo);
        stubPresentations(firstTrial, secondTrial, thirdTrial);

        // A fresh service and shuffler stand in for a restarted server: the
        // derivation must continue exactly where the session left off.
        GameService restartedService = new GameService(
                appUserRepository,
                gameSessionRepository,
                trialRepository,
                presentationRepository,
                playerAnswerRepository,
                new GameMapper(),
                new RoundShuffler(),
                new LadderFloors(),
                roundSources()
        );

        RoundResponse response = restartedService.getNextRound(userDetails, SESSION_UUID);

        DerivedRound expected = derivedOrder.get(2);
        assertEquals(expected.getTrial().getId(), response.getRoundId());
        assertEquals(expected.getTarget().getGloss(), response.getTargetTranslation());
        assertEquals(expected.getLeft().getId(), response.getLeft().getIdeophoneId());
        assertEquals(expected.getRight().getId(), response.getRight().getIdeophoneId());
    }

    @Test
    void getNextRoundReturnsCompletionResponseAfterAllRoundsAreAnswered() {
        Trial answeredTrial = trial(
                100L,
                word(1L, "ごそごそ", "gosogoso", "with a rustling sound", "audio/a0h-gosogoso.m4a"),
                word(2L, "かたかた", "katakata", "clattering, rattling", "audio/a0k-katakata.m4a")
        );
        when(gameSessionRepository.findBySessionUuid(SESSION_UUID)).thenReturn(Optional.of(session));
        when(trialRepository.findScoredChoosingTrials()).thenReturn(List.of(answeredTrial));
        when(playerAnswerRepository.findAnsweredTrialIdsBySessionId(20L)).thenReturn(List.of(100L));

        RoundResponse response = gameService.getNextRound(userDetails, SESSION_UUID);

        assertTrue(response.isCompleted());
        assertEquals("Game session is complete", response.getMessage());
        assertEquals(SESSION_UUID, response.getSessionUuid());
        assertEquals(ConditionName.CONDITION_1_SOKUON, response.getConditionName());
        assertNull(session.getCompletedAt());
    }

    @Test
    void submitAnswerJudgesAgainstDerivedTargetAndStoresIt() {
        Word left = word(1L, "ごそごそ", "gosogoso", "with a rustling sound", "audio/a0h-gosogoso.m4a");
        Word right = word(2L, "かたかた", "katakata", "clattering, rattling", "audio/a0k-katakata.m4a");
        Trial trial = trial(100L, left, right);
        // A second scored trial keeps the session incomplete after one answer (completion
        // is now the mode's scored-round count, not a separate counter).
        Trial filler = trial(101L,
                word(3L, "しとしと", "sitosito", "drizzling", "audio/a1h-sitosito.m4a"),
                word(4L, "ばちゃばちゃ", "batyabatya", "splashing", "audio/a1k-batyabatya.m4a"));
        List<Trial> scored = List.of(trial, filler);
        DerivedRound derived = derivedScoredRound(scored, 100L);
        SubmitAnswerRequest request = new SubmitAnswerRequest();
        request.setRoundId(100L);
        request.setSelectedIdeophoneId(derived.getTarget().getId());
        request.setResponseTimeMs(1234);
        when(gameSessionRepository.findBySessionUuid(SESSION_UUID)).thenReturn(Optional.of(session));
        when(trialRepository.findByIdWithPairingWords(100L)).thenReturn(Optional.of(trial));
        when(playerAnswerRepository.existsBySessionIdAndTrialId(20L, 100L)).thenReturn(false);
        when(trialRepository.findScoredChoosingTrials()).thenReturn(scored);
        when(playerAnswerRepository.countBySessionId(20L)).thenReturn(1L);
        when(playerAnswerRepository.countBySessionIdAndCorrectTrue(20L)).thenReturn(1L);

        AnswerResultResponse response = gameService.submitAnswer(userDetails, SESSION_UUID, request);

        ArgumentCaptor<PlayerAnswer> answerCaptor = ArgumentCaptor.forClass(PlayerAnswer.class);
        verify(playerAnswerRepository).saveAndFlush(answerCaptor.capture());
        PlayerAnswer savedAnswer = answerCaptor.getValue();
        assertEquals(session, savedAnswer.getSession());
        assertEquals(trial, savedAnswer.getTrial());
        assertEquals(derived.getTarget(), savedAnswer.getSelectedWord());
        assertEquals(derived.getTarget(), savedAnswer.getTargetWord());
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
        Word left = word(1L, "ごそごそ", "gosogoso", "with a rustling sound", "audio/a0h-gosogoso.m4a");
        Word right = word(2L, "かたかた", "katakata", "clattering, rattling", "audio/a0k-katakata.m4a");
        Trial trial = trial(100L, left, right);
        DerivedRound derived = derivedScoredRound(List.of(trial), 100L);
        SubmitAnswerRequest request = new SubmitAnswerRequest();
        request.setRoundId(100L);
        request.setSelectedIdeophoneId(derived.getOther().getId());
        request.setResponseTimeMs(1234);
        when(gameSessionRepository.findBySessionUuid(SESSION_UUID)).thenReturn(Optional.of(session));
        when(trialRepository.findByIdWithPairingWords(100L)).thenReturn(Optional.of(trial));
        when(playerAnswerRepository.existsBySessionIdAndTrialId(20L, 100L)).thenReturn(false);
        when(trialRepository.findScoredChoosingTrials()).thenReturn(List.of(trial));
        when(playerAnswerRepository.countBySessionId(20L)).thenReturn(1L);
        when(playerAnswerRepository.countBySessionIdAndCorrectTrue(20L)).thenReturn(0L);

        AnswerResultResponse response = gameService.submitAnswer(userDetails, SESSION_UUID, request);

        ArgumentCaptor<PlayerAnswer> answerCaptor = ArgumentCaptor.forClass(PlayerAnswer.class);
        verify(playerAnswerRepository).saveAndFlush(answerCaptor.capture());
        PlayerAnswer savedAnswer = answerCaptor.getValue();
        assertEquals(derived.getOther(), savedAnswer.getSelectedWord());
        assertEquals(derived.getTarget(), savedAnswer.getTargetWord());
        assertFalse(savedAnswer.isCorrect());
        assertFalse(response.isCorrect());
        assertEquals(derived.getTarget().getId(), response.getCorrectIdeophoneId());
    }

    @Test
    void submitAnswerMarksSessionCompleteWhenLastRoundIsAnswered() {
        Word left = word(1L, "ごそごそ", "gosogoso", "with a rustling sound", "audio/a0h-gosogoso.m4a");
        Word right = word(2L, "かたかた", "katakata", "clattering, rattling", "audio/a0k-katakata.m4a");
        Trial trial = trial(100L, left, right);
        SubmitAnswerRequest request = new SubmitAnswerRequest();
        request.setRoundId(100L);
        request.setSelectedIdeophoneId(1L);
        request.setResponseTimeMs(1234);
        when(gameSessionRepository.findBySessionUuid(SESSION_UUID)).thenReturn(Optional.of(session));
        when(trialRepository.findByIdWithPairingWords(100L)).thenReturn(Optional.of(trial));
        when(playerAnswerRepository.existsBySessionIdAndTrialId(20L, 100L)).thenReturn(false);
        // One scored trial, one answer -> the session completes.
        when(trialRepository.findScoredChoosingTrials()).thenReturn(List.of(trial));
        when(playerAnswerRepository.countBySessionId(20L)).thenReturn(1L);
        when(playerAnswerRepository.countBySessionIdAndCorrectTrue(20L)).thenReturn(1L);

        AnswerResultResponse response = gameService.submitAnswer(userDetails, SESSION_UUID, request);

        assertEquals(1L, response.getTotalAnswered());
        assertEquals(1L, response.getTotalCorrect());
        assertTrue(session.getCompletedAt() != null);
    }

    @Test
    void submitAnswerTranslatesConcurrentDuplicateInsertToConflict() {
        Word left = word(1L, "ごそごそ", "gosogoso", "with a rustling sound", "audio/a0h-gosogoso.m4a");
        Word right = word(2L, "かたかた", "katakata", "clattering, rattling", "audio/a0k-katakata.m4a");
        Trial trial = trial(100L, left, right);
        SubmitAnswerRequest request = new SubmitAnswerRequest();
        request.setRoundId(100L);
        request.setSelectedIdeophoneId(1L);
        request.setResponseTimeMs(1234);
        when(gameSessionRepository.findBySessionUuid(SESSION_UUID)).thenReturn(Optional.of(session));
        when(trialRepository.findByIdWithPairingWords(100L)).thenReturn(Optional.of(trial));
        when(playerAnswerRepository.existsBySessionIdAndTrialId(20L, 100L)).thenReturn(false);
        when(trialRepository.findScoredChoosingTrials()).thenReturn(List.of(trial));
        when(playerAnswerRepository.saveAndFlush(any(PlayerAnswer.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(ConflictException.class, () -> gameService.submitAnswer(userDetails, SESSION_UUID, request));
        assertNull(session.getCompletedAt());
    }

    @Test
    void submitAnswerRejectsWordThatIsNotAChoiceForTheRound() {
        Trial trial = trial(
                100L,
                word(1L, "ごそごそ", "gosogoso", "with a rustling sound", "audio/a0h-gosogoso.m4a"),
                word(2L, "かたかた", "katakata", "clattering, rattling", "audio/a0k-katakata.m4a")
        );
        SubmitAnswerRequest request = new SubmitAnswerRequest();
        request.setRoundId(100L);
        request.setSelectedIdeophoneId(999L);
        request.setResponseTimeMs(500);
        when(gameSessionRepository.findBySessionUuid(SESSION_UUID)).thenReturn(Optional.of(session));
        when(trialRepository.findByIdWithPairingWords(100L)).thenReturn(Optional.of(trial));
        when(playerAnswerRepository.existsBySessionIdAndTrialId(20L, 100L)).thenReturn(false);
        when(trialRepository.findScoredChoosingTrials()).thenReturn(List.of(trial));

        assertThrows(BadRequestException.class, () -> gameService.submitAnswer(userDetails, SESSION_UUID, request));
        verify(playerAnswerRepository, never()).saveAndFlush(any(PlayerAnswer.class));
    }

    @Test
    void getNextRoundServesPracticeRoundsBeforeScoredRounds() {
        session.setIncludePractice(true);
        Trial firstPractice = practiceTrial(
                900L,
                word(31L, "そっと", "sotto", "softly, gently", "audio/p0h-sotto.m4a"),
                word(32L, "がたん", "gataN", "with a bang", "audio/p0k-gataN.m4a")
        );
        Trial secondPractice = practiceTrial(
                901L,
                word(33L, "じっと", "zitto", "motionless, fixedly", "audio/p1h-zitto.m4a"),
                word(34L, "ぱっ", "paQ", "suddenly, in a flash", "audio/p1k-paQ.m4a")
        );
        when(gameSessionRepository.findBySessionUuid(SESSION_UUID)).thenReturn(Optional.of(session));
        when(trialRepository.findByPracticeTrueOrderByIdAsc()).thenReturn(List.of(firstPractice, secondPractice));
        stubPresentations(firstPractice, secondPractice);

        RoundResponse response = gameService.getNextRound(userDetails, SESSION_UUID);

        assertEquals(900L, response.getRoundId());
        assertTrue(response.isPractice());
        verify(trialRepository, never()).findScoredChoosingTrials();
    }

    @Test
    void submitPracticeAnswerReturnsFeedbackWithoutPersistingAnswer() {
        session.setIncludePractice(true);
        Word left = word(31L, "そっと", "sotto", "softly, gently", "audio/p0h-sotto.m4a");
        Word right = word(32L, "がたん", "gataN", "with a bang", "audio/p0k-gataN.m4a");
        Trial practiceTrial = practiceTrial(900L, left, right);
        DerivedRound derived = roundShuffler
                .derivePracticeRounds(session.getShuffleSeed(), List.of(practiceTrial))
                .get(0);
        SubmitAnswerRequest request = new SubmitAnswerRequest();
        request.setRoundId(900L);
        request.setSelectedIdeophoneId(derived.getTarget().getId());
        request.setResponseTimeMs(1234);
        when(gameSessionRepository.findBySessionUuid(SESSION_UUID)).thenReturn(Optional.of(session));
        when(trialRepository.findByIdWithPairingWords(900L)).thenReturn(Optional.of(practiceTrial));
        when(trialRepository.findByPracticeTrueOrderByIdAsc()).thenReturn(List.of(practiceTrial));
        when(playerAnswerRepository.countBySessionId(20L)).thenReturn(0L);
        when(playerAnswerRepository.countBySessionIdAndCorrectTrue(20L)).thenReturn(0L);

        AnswerResultResponse response = gameService.submitAnswer(userDetails, SESSION_UUID, request);

        assertTrue(response.isPractice());
        assertTrue(response.isCorrect());
        assertEquals(0L, response.getTotalAnswered());
        assertEquals(0L, response.getTotalCorrect());
        assertEquals(1, session.getPracticeAnswered());
        assertNull(session.getCompletedAt());
        verify(playerAnswerRepository, never()).saveAndFlush(any(PlayerAnswer.class));
    }

    @Test
    void submitPracticeAnswerRejectsOutOfOrderAndRepeatedRounds() {
        session.setIncludePractice(true);
        Trial firstPractice = practiceTrial(
                900L,
                word(31L, "そっと", "sotto", "softly, gently", "audio/p0h-sotto.m4a"),
                word(32L, "がたん", "gataN", "with a bang", "audio/p0k-gataN.m4a")
        );
        Trial secondPractice = practiceTrial(
                901L,
                word(33L, "じっと", "zitto", "motionless, fixedly", "audio/p1h-zitto.m4a"),
                word(34L, "ぱっ", "paQ", "suddenly, in a flash", "audio/p1k-paQ.m4a")
        );
        when(gameSessionRepository.findBySessionUuid(SESSION_UUID)).thenReturn(Optional.of(session));
        when(trialRepository.findByIdWithPairingWords(901L)).thenReturn(Optional.of(secondPractice));
        when(trialRepository.findByPracticeTrueOrderByIdAsc()).thenReturn(List.of(firstPractice, secondPractice));

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
        verify(playerAnswerRepository, never()).saveAndFlush(any(PlayerAnswer.class));
    }

    // The served round needs a presentation per word for the mapper; the map is
    // keyed by word id, so any() args suffice.
    private void stubPresentations(Trial... trials) {
        List<Presentation> presentations = new java.util.ArrayList<>();
        for (Trial trial : trials) {
            presentations.add(new Presentation(trial.getPairing().getWordA(), ConditionName.CONDITION_1_SOKUON,
                    trial.getPairing().getWordA().getCanonicalForm(), "HU"));
            presentations.add(new Presentation(trial.getPairing().getWordB(), ConditionName.CONDITION_1_SOKUON,
                    trial.getPairing().getWordB().getCanonicalForm(), "KD"));
        }
        when(presentationRepository.findByWordIdInAndConditionName(any(), any())).thenReturn(presentations);
    }

    private DerivedRound derivedScoredRound(List<Trial> trials, Long trialId) {
        return roundShuffler.deriveScoredRounds(session.getShuffleSeed(), trials).stream()
                .filter(derived -> derived.getTrial().getId().equals(trialId))
                .findFirst()
                .orElseThrow();
    }

    private Trial trial(Long id, Word wordA, Word wordB) {
        Trial trial = new Trial(pairing(wordA, wordB), wordA, false);
        setId(trial, id);
        return trial;
    }

    // A trial whose pairing carries a specific pair code, as the ladder floors key on.
    private Trial ladderTrial(Long id, String pairCode, Word wordA, Word wordB) {
        Trial trial = new Trial(
                new Pairing(pairCode, null, wordA, wordB, Modality.AUDITORY, true, "THESIS"), wordA, false);
        setId(trial, id);
        return trial;
    }

    private Trial practiceTrial(Long id, Word wordA, Word wordB) {
        Trial trial = new Trial(pairing(wordA, wordB), wordA, true);
        setId(trial, id);
        return trial;
    }

    private Pairing pairing(Word wordA, Word wordB) {
        return new Pairing("code", null, wordA, wordB, Modality.AUDITORY, true, "THESIS");
    }

    private Word word(Long id, String kana, String romaji, String gloss, String stimulusFile) {
        Word word = new Word(null, romaji, kana, kana, "H", gloss, Modality.AUDITORY, stimulusFile);
        setId(word, id);
        return word;
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
