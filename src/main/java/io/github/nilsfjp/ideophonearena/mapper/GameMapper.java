package io.github.nilsfjp.ideophonearena.mapper;

import io.github.nilsfjp.ideophonearena.dto.AnswerResultResponse;
import io.github.nilsfjp.ideophonearena.dto.AttemptResponse;
import io.github.nilsfjp.ideophonearena.dto.GameSessionResponse;
import io.github.nilsfjp.ideophonearena.dto.IdeophoneChoiceResponse;
import io.github.nilsfjp.ideophonearena.dto.LeaderboardEntryResponse;
import io.github.nilsfjp.ideophonearena.dto.LeaderboardPageResponse;
import io.github.nilsfjp.ideophonearena.dto.RoundResponse;
import io.github.nilsfjp.ideophonearena.dto.TimingResponse;
import io.github.nilsfjp.ideophonearena.dto.TranslationResponse;
import io.github.nilsfjp.ideophonearena.model.ArenaRound;
import io.github.nilsfjp.ideophonearena.model.DerivedRound;
import io.github.nilsfjp.ideophonearena.model.GameSession;
import io.github.nilsfjp.ideophonearena.model.Ideophone;
import io.github.nilsfjp.ideophonearena.model.PlayerAnswer;
import io.github.nilsfjp.ideophonearena.repository.LeaderboardEntryProjection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class GameMapper {

    private static final int FIXATION_MS = 800;
    private static final int PRE_CHOICE_DELAY_MS = 0;
    private static final String STIMULUS_URL_PREFIX = "/stimuli/";

    public GameSessionResponse toSessionResponse(GameSession session) {
        return new GameSessionResponse(
                session.getSessionUuid(),
                session.getDifficultyLevel(),
                session.getConditionName(),
                session.isIncludePractice(),
                session.getStartedAt()
        );
    }

    // The served prompt, translations, and sides all come from the session's
    // seed-derived presentation, not from the arena_rounds row (whose prompt
    // and correct_ideophone_id document the fixed thesis target).
    public RoundResponse toRoundResponse(GameSession session, DerivedRound derivedRound) {
        ArenaRound round = derivedRound.getRound();
        return new RoundResponse(
                session.getSessionUuid(),
                round.getId(),
                derivedRound.getTarget().getGloss(),
                round.getConditionName(),
                round.getDifficultyLevel(),
                round.isPractice(),
                new TranslationResponse(derivedRound.getTarget().getGloss(), derivedRound.getOther().getGloss()),
                toIdeophoneResponse(derivedRound.getLeft()),
                toIdeophoneResponse(derivedRound.getRight()),
                new TimingResponse(FIXATION_MS, PRE_CHOICE_DELAY_MS)
        );
    }

    public RoundResponse toCompletedRoundResponse(GameSession session, String message) {
        return new RoundResponse(true, message, session.getSessionUuid(), null, null,
                session.getConditionName(), session.getDifficultyLevel(), false, null, null, null, null);
    }

    public AnswerResultResponse toAnswerResultResponse(DerivedRound derivedRound, Ideophone selectedIdeophone,
            PlayerAnswer answer, long totalAnswered, long totalCorrect) {
        return new AnswerResultResponse(
                derivedRound.getRound().getId(),
                selectedIdeophone.getId(),
                derivedRound.getTarget().getId(),
                answer.isCorrect(),
                false,
                derivedRound.getTarget().getGloss(),
                derivedRound.getTarget().getKana(),
                selectedIdeophone.getKana(),
                totalAnswered,
                totalCorrect
        );
    }

    // Practice answers are never persisted, so there is no PlayerAnswer to map
    // from; totals stay the session's scored counts.
    public AnswerResultResponse toPracticeAnswerResultResponse(DerivedRound derivedRound, Ideophone selectedIdeophone,
            boolean correct, long totalAnswered, long totalCorrect) {
        return new AnswerResultResponse(
                derivedRound.getRound().getId(),
                selectedIdeophone.getId(),
                derivedRound.getTarget().getId(),
                correct,
                true,
                derivedRound.getTarget().getGloss(),
                derivedRound.getTarget().getKana(),
                selectedIdeophone.getKana(),
                totalAnswered,
                totalCorrect
        );
    }

    // History replays what the session actually asked: the stored derived
    // target, not the round row's thesis target.
    public AttemptResponse toAttemptResponse(PlayerAnswer answer) {
        return new AttemptResponse(
                answer.getAnsweredAt(),
                answer.getTargetIdeophone().getGloss(),
                answer.getSelectedIdeophone().getKana(),
                answer.getTargetIdeophone().getKana(),
                answer.isCorrect(),
                answer.getResponseTimeMs()
        );
    }

    public LeaderboardPageResponse toLeaderboardPageResponse(Page<LeaderboardEntryProjection> page) {
        List<LeaderboardEntryResponse> entries = page.getContent().stream()
                .map(this::toLeaderboardEntryResponse)
                .toList();
        return new LeaderboardPageResponse(entries, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }

    private LeaderboardEntryResponse toLeaderboardEntryResponse(LeaderboardEntryProjection projection) {
        long bestSessionCorrect = valueOrZero(projection.getBestSessionCorrect());
        long bestSessionAnswered = valueOrZero(projection.getBestSessionAnswered());
        double bestSessionAccuracy = bestSessionAnswered == 0
                ? 0.0
                : (double) bestSessionCorrect / bestSessionAnswered;
        return new LeaderboardEntryResponse(projection.getUsername(), bestSessionCorrect, bestSessionAnswered,
                bestSessionAccuracy);
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private IdeophoneChoiceResponse toIdeophoneResponse(Ideophone ideophone) {
        return new IdeophoneChoiceResponse(
                ideophone.getId(),
                ideophone.getKana(),
                ideophone.getDisplayForm(),
                ideophone.getCanonicalForm(),
                ideophone.getRomaji(),
                ideophone.getStimulusFile(),
                STIMULUS_URL_PREFIX + ideophone.getStimulusFile(),
                ideophone.getModality(),
                ideophone.getCanonicalScript()
        );
    }
}
