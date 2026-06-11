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
                session.getStartedAt()
        );
    }

    public RoundResponse toRoundResponse(GameSession session, ArenaRound round) {
        return new RoundResponse(
                session.getSessionUuid(),
                round.getId(),
                round.getPrompt(),
                round.getConditionName(),
                round.getDifficultyLevel(),
                toTranslationResponse(round),
                toIdeophoneResponse(round.getLeftIdeophone()),
                toIdeophoneResponse(round.getRightIdeophone()),
                new TimingResponse(FIXATION_MS, PRE_CHOICE_DELAY_MS)
        );
    }

    public RoundResponse toCompletedRoundResponse(GameSession session, String message) {
        return new RoundResponse(true, message, session.getSessionUuid(), null, null,
                session.getConditionName(), session.getDifficultyLevel(), null, null, null, null);
    }

    public AnswerResultResponse toAnswerResultResponse(ArenaRound round, Ideophone selectedIdeophone,
            PlayerAnswer answer, long totalAnswered, long totalCorrect) {
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

    public AttemptResponse toAttemptResponse(PlayerAnswer answer) {
        return new AttemptResponse(
                answer.getAnsweredAt(),
                answer.getRound().getPrompt(),
                answer.getSelectedIdeophone().getKana(),
                answer.getRound().getCorrectIdeophone().getKana(),
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
        long totalAnswered = valueOrZero(projection.getTotalAnswers());
        long totalCorrect = valueOrZero(projection.getCorrectAnswers());
        double accuracy = totalAnswered == 0 ? 0.0 : (double) totalCorrect / totalAnswered;
        return new LeaderboardEntryResponse(projection.getUsername(), totalAnswered, totalCorrect, accuracy);
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private TranslationResponse toTranslationResponse(ArenaRound round) {
        String target = round.getPrompt();
        String other = round.getLeftIdeophone().getGloss().equals(target)
                ? round.getRightIdeophone().getGloss()
                : round.getLeftIdeophone().getGloss();
        return new TranslationResponse(target, other);
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
