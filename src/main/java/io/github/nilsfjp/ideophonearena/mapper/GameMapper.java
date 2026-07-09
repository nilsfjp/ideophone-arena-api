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
import io.github.nilsfjp.ideophonearena.model.DerivedRound;
import io.github.nilsfjp.ideophonearena.model.GameSession;
import io.github.nilsfjp.ideophonearena.model.PlayerAnswer;
import io.github.nilsfjp.ideophonearena.model.Presentation;
import io.github.nilsfjp.ideophonearena.model.Trial;
import io.github.nilsfjp.ideophonearena.model.Word;
import io.github.nilsfjp.ideophonearena.repository.LeaderboardEntryProjection;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class GameMapper {

    private static final int FIXATION_MS = 800;
    private static final int PRE_CHOICE_DELAY_MS = 0;
    private static final String STIMULUS_URL_PREFIX = "/stimuli/";

    // totalRounds is derived from the RoundSource seam, not from the entity, so the
    // service supplies it rather than the mapper reaching for a repository.
    public GameSessionResponse toSessionResponse(GameSession session, int totalRounds) {
        return new GameSessionResponse(
                session.getSessionUuid(),
                session.getConditionName(),
                session.getGameMode(),
                session.getLadderFloor(),
                session.isIncludePractice(),
                session.getStartedAt(),
                totalRounds
        );
    }

    // The served prompt, translations, and sides all come from the session's
    // seed-derived presentation. Condition is a session fact (it left the trial in
    // the ADR-3 collapse). Each card's display_form comes from
    // presentation(word, condition); everything else is a word-level fact.
    public RoundResponse toRoundResponse(GameSession session, DerivedRound derivedRound,
            Map<Long, Presentation> presentationsByWordId) {
        Trial trial = derivedRound.getTrial();
        return new RoundResponse(
                session.getSessionUuid(),
                trial.getId(),
                derivedRound.getTarget().getGloss(),
                session.getConditionName(),
                trial.isPractice(),
                derivedRound.isTargetMeaningListedFirst(),
                new TranslationResponse(derivedRound.getTarget().getGloss(), derivedRound.getOther().getGloss()),
                toIdeophoneResponse(derivedRound.getLeft(), presentationsByWordId),
                toIdeophoneResponse(derivedRound.getRight(), presentationsByWordId),
                new TimingResponse(FIXATION_MS, PRE_CHOICE_DELAY_MS)
        );
    }

    public RoundResponse toCompletedRoundResponse(GameSession session, String message) {
        return new RoundResponse(true, message, session.getSessionUuid(), null, null,
                session.getConditionName(), false, false, null, null, null, null);
    }

    public AnswerResultResponse toAnswerResultResponse(DerivedRound derivedRound, Word selectedWord,
            PlayerAnswer answer, long totalAnswered, long totalCorrect) {
        return new AnswerResultResponse(
                derivedRound.getTrial().getId(),
                selectedWord.getId(),
                derivedRound.getTarget().getId(),
                answer.isCorrect(),
                false,
                derivedRound.getTarget().getGloss(),
                derivedRound.getTarget().getKana(),
                selectedWord.getKana(),
                totalAnswered,
                totalCorrect
        );
    }

    // Practice answers are never persisted, so there is no PlayerAnswer to map
    // from; totals stay the session's scored counts.
    public AnswerResultResponse toPracticeAnswerResultResponse(DerivedRound derivedRound, Word selectedWord,
            boolean correct, long totalAnswered, long totalCorrect) {
        return new AnswerResultResponse(
                derivedRound.getTrial().getId(),
                selectedWord.getId(),
                derivedRound.getTarget().getId(),
                correct,
                true,
                derivedRound.getTarget().getGloss(),
                derivedRound.getTarget().getKana(),
                selectedWord.getKana(),
                totalAnswered,
                totalCorrect
        );
    }

    // History replays what the session actually asked: the stored derived
    // target, not the trial row's thesis target.
    public AttemptResponse toAttemptResponse(PlayerAnswer answer) {
        return new AttemptResponse(
                answer.getAnsweredAt(),
                answer.getTargetWord().getGloss(),
                answer.getSelectedWord().getKana(),
                answer.getTargetWord().getKana(),
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

    // The card renders word-level facts (kana, canonical form, romaji, audio,
    // modality) plus this session-condition's script manipulation: display_form
    // (invariant 1, verbatim, from presentation(word, condition)). The id keeps its
    // frozen name ideophoneId (= the word id).
    private IdeophoneChoiceResponse toIdeophoneResponse(Word word, Map<Long, Presentation> presentationsByWordId) {
        Presentation presentation = presentationsByWordId.get(word.getId());
        return new IdeophoneChoiceResponse(
                word.getId(),
                word.getKana(),
                presentation.getDisplayForm(),
                word.getCanonicalForm(),
                word.getRomaji(),
                word.getStimulusFile(),
                STIMULUS_URL_PREFIX + word.getStimulusFile(),
                word.getModality()
        );
    }
}
