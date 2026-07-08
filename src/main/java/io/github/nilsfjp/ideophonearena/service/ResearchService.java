package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.dto.DivergenceResponse;
import io.github.nilsfjp.ideophonearena.dto.PositionBiasResponse;
import io.github.nilsfjp.ideophonearena.dto.RatingDistributionsResponse;
import io.github.nilsfjp.ideophonearena.mapper.ResearchMapper;
import io.github.nilsfjp.ideophonearena.model.DerivedRound;
import io.github.nilsfjp.ideophonearena.model.GameSession;
import io.github.nilsfjp.ideophonearena.model.PlayerAnswer;
import io.github.nilsfjp.ideophonearena.model.Trial;
import io.github.nilsfjp.ideophonearena.model.Word;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import io.github.nilsfjp.ideophonearena.repository.IdeophoneGuessStatsProjection;
import io.github.nilsfjp.ideophonearena.repository.IdeophoneRatingStatsProjection;
import io.github.nilsfjp.ideophonearena.repository.ModalityRatingDistributionProjection;
import io.github.nilsfjp.ideophonearena.repository.PlayerAnswerRepository;
import io.github.nilsfjp.ideophonearena.repository.RatingRepository;
import io.github.nilsfjp.ideophonearena.repository.TrialRepository;
import io.github.nilsfjp.ideophonearena.repository.WordRepository;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResearchService {

    private static final Logger log = LoggerFactory.getLogger(ResearchService.class);

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 7;
    private static final int RATING_BINS = MAX_RATING - MIN_RATING + 1;

    private final PlayerAnswerRepository playerAnswerRepository;
    private final RatingRepository ratingRepository;
    private final WordRepository wordRepository;
    private final TrialRepository trialRepository;
    private final RoundShuffler roundShuffler;
    private final PositionBiasCalculator positionBiasCalculator;
    private final ResearchMapper researchMapper;

    public ResearchService(PlayerAnswerRepository playerAnswerRepository, RatingRepository ratingRepository,
            WordRepository wordRepository, TrialRepository trialRepository,
            RoundShuffler roundShuffler, PositionBiasCalculator positionBiasCalculator,
            ResearchMapper researchMapper) {
        this.playerAnswerRepository = playerAnswerRepository;
        this.ratingRepository = ratingRepository;
        this.wordRepository = wordRepository;
        this.trialRepository = trialRepository;
        this.roundShuffler = roundShuffler;
        this.positionBiasCalculator = positionBiasCalculator;
        this.researchMapper = researchMapper;
    }

    // Live divergence: the crowd of real players (Rider A excludes automation and
    // the thesis_p% cohort). One row per word with at least one guess or rating.
    @Transactional(readOnly = true)
    public List<DivergenceResponse> getDivergence() {
        return mergeDivergence(
                playerAnswerRepository.aggregateGuessStatsByWord(),
                ratingRepository.aggregateRatingStatsByWord());
    }

    // The Observatory thesis layer (NIL-54): the same per-word merge over the
    // thesis_p% cohort only (inverted Rider A predicate). Each of the 30 target
    // words was answered by all 36 thesis participants, so the per-word guess
    // accuracy reproduces pairings.thesis_accuracy and rolls up to the vendored
    // per-modality figures (68.6/64.2/59.7).
    @Transactional(readOnly = true)
    public List<DivergenceResponse> getThesisDivergence() {
        return mergeDivergence(
                playerAnswerRepository.aggregateThesisGuessStatsByWord(),
                ratingRepository.aggregateThesisRatingStatsByWord());
    }

    // Guess accuracy (from player_answers) and mean rating (from ratings) are
    // independent aggregates; a single join across both would form a cartesian
    // product and inflate the correct-answer sum. So each is queried separately
    // and merged on the word id, mirroring AdminStatsService. Both aggregates
    // are word-keyed (ADR-0): one row per word, healing the pre-M2 row split.
    private List<DivergenceResponse> mergeDivergence(List<IdeophoneGuessStatsProjection> guessRows,
            List<IdeophoneRatingStatsProjection> ratingRows) {
        Map<Long, IdeophoneGuessStatsProjection> guessStats = new LinkedHashMap<>();
        for (IdeophoneGuessStatsProjection row : guessRows) {
            guessStats.put(row.getIdeophoneId(), row);
        }

        Map<Long, IdeophoneRatingStatsProjection> ratingStats = new LinkedHashMap<>();
        for (IdeophoneRatingStatsProjection row : ratingRows) {
            ratingStats.put(row.getIdeophoneId(), row);
        }

        // One row per word that has at least one guess or one rating, ordered by
        // id for a deterministic response.
        TreeSet<Long> wordIds = new TreeSet<>();
        wordIds.addAll(guessStats.keySet());
        wordIds.addAll(ratingStats.keySet());

        Map<Long, Word> words = new LinkedHashMap<>();
        for (Word word : wordRepository.findAllById(wordIds)) {
            words.put(word.getId(), word);
        }

        List<DivergenceResponse> divergence = new ArrayList<>();
        for (Long wordId : wordIds) {
            Word word = words.get(wordId);
            if (word == null) {
                continue;
            }
            IdeophoneGuessStatsProjection guess = guessStats.get(wordId);
            long guessCount = guess == null ? 0L : valueOrZero(guess.getGuesses());
            long correct = guess == null ? 0L : valueOrZero(guess.getCorrect());

            IdeophoneRatingStatsProjection rating = ratingStats.get(wordId);
            long ratingCount = rating == null ? 0L : valueOrZero(rating.getRatingCount());
            Double meanRating = rating == null ? null : rating.getMeanRating();

            divergence.add(researchMapper.toDivergenceResponse(word, guessCount, correct, ratingCount, meanRating));
        }
        return divergence;
    }

    // Rainclouds need per-value counts, not means. Each modality that has any
    // ratings gets a dense 1-7 grid (zero-filled by the mapper) plus its total
    // n; modalities with no ratings are omitted. Word-keyed (ADR-0); the
    // response shape is unchanged.
    @Transactional(readOnly = true)
    public RatingDistributionsResponse getRatingDistributions() {
        Map<Modality, long[]> countsByModality = new EnumMap<>(Modality.class);
        Map<Modality, Long> nByModality = new EnumMap<>(Modality.class);
        for (ModalityRatingDistributionProjection row : ratingRepository.aggregateRatingDistribution()) {
            Modality modality = row.getModality();
            Short ratingValue = row.getRatingValue();
            if (modality == null || ratingValue == null
                    || ratingValue < MIN_RATING || ratingValue > MAX_RATING) {
                // Defensive: the query already excludes null modality, and
                // ratings are constrained to 1..7 at write time.
                continue;
            }
            long count = valueOrZero(row.getCount());
            long[] bins = countsByModality.computeIfAbsent(modality, unused -> new long[RATING_BINS]);
            bins[ratingValue - MIN_RATING] += count;
            nByModality.merge(modality, count, Long::sum);
        }
        return researchMapper.toRatingDistributions(countsByModality, nByModality);
    }

    // Fairness of the forced choice, reconstructed without a schema change by
    // replaying each session's deterministic shuffle (RoundShuffler) against its
    // stored answers. The shuffle draws two dissociated axes: the target's
    // left/right card side and whether the target's meaning is listed top or
    // bottom. We measure a left/right pick rate + d'/criterion on the side axis,
    // and accuracy split by the target-meaning position on the vertical axis.
    @Transactional(readOnly = true)
    public PositionBiasResponse getPositionBias() {
        // Trials are condition-free (ADR-3), so the scored base list is the same
        // for every CHOOSING session -- fetch it once and derive per session (each
        // shuffle seed differs). The shuffle algorithm is byte-for-byte unchanged
        // (Rider B); only the joins moved to word grain. findScoredChoosingTrials
        // excludes the HAPTIC ladder trials, so this replay stays the frozen 47-pool.
        List<Trial> scoredTrials = trialRepository.findScoredChoosingTrials();
        Map<Long, Map<Long, DerivedRound>> derivedBySession = new HashMap<>();

        long n = 0;
        long leftPickCount = 0;
        long signalTrials = 0;      // target on the left: respond-left == correct
        long hits = 0;              // of those, answered left
        long noiseTrials = 0;       // target on the right
        long falseAlarms = 0;       // of those, still answered left
        long targetTopN = 0;
        long targetTopCorrect = 0;
        long targetBottomN = 0;
        long targetBottomCorrect = 0;
        long skipped = 0;

        for (PlayerAnswer answer : playerAnswerRepository.findScoredForPositionBias()) {
            GameSession session = answer.getSession();
            Map<Long, DerivedRound> derivedByTrialId =
                    derivedBySession.computeIfAbsent(session.getId(), unused -> deriveByTrialId(session, scoredTrials));

            DerivedRound derived = derivedByTrialId.get(answer.getTrial().getId());
            if (derived == null) {
                // The answer's trial is not in the derived scored set (data
                // drift). Skip rather than silently miscount.
                skipped++;
                continue;
            }

            long selectedId = answer.getSelectedWord().getId();
            long targetId = answer.getTargetWord().getId();
            boolean pickedLeft = derived.getLeft().getId().equals(selectedId);
            boolean targetLeft = derived.getLeft().getId().equals(targetId);
            boolean correct = answer.isCorrect();

            n++;
            if (pickedLeft) {
                leftPickCount++;
            }
            if (targetLeft) {
                signalTrials++;
                if (pickedLeft) {
                    hits++;
                }
            } else {
                noiseTrials++;
                if (pickedLeft) {
                    falseAlarms++;
                }
            }
            if (derived.isTargetMeaningListedFirst()) {
                targetTopN++;
                if (correct) {
                    targetTopCorrect++;
                }
            } else {
                targetBottomN++;
                if (correct) {
                    targetBottomCorrect++;
                }
            }
        }

        if (skipped > 0) {
            log.warn("position-bias: skipped {} answer(s) whose trial was absent from the derived scored set", skipped);
        }

        PositionBiasCalculator.SignalDetection sdt =
                positionBiasCalculator.signalDetection(hits, signalTrials, falseAlarms, noiseTrials);
        return researchMapper.toPositionBiasResponse(n, leftPickCount, sdt.getDPrime(), sdt.getCriterion(),
                targetTopN, targetTopCorrect, targetBottomN, targetBottomCorrect);
    }

    private Map<Long, DerivedRound> deriveByTrialId(GameSession session, List<Trial> scoredTrials) {
        Map<Long, DerivedRound> byTrialId = new HashMap<>();
        for (DerivedRound derived : roundShuffler.deriveScoredRounds(session.getShuffleSeed(), scoredTrials)) {
            byTrialId.put(derived.getTrial().getId(), derived);
        }
        return byTrialId;
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
