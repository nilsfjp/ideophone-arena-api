package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.dto.DivergenceResponse;
import io.github.nilsfjp.ideophonearena.dto.PositionBiasResponse;
import io.github.nilsfjp.ideophonearena.dto.RatingDistributionsResponse;
import io.github.nilsfjp.ideophonearena.mapper.ResearchMapper;
import io.github.nilsfjp.ideophonearena.model.ArenaRound;
import io.github.nilsfjp.ideophonearena.model.DerivedRound;
import io.github.nilsfjp.ideophonearena.model.GameSession;
import io.github.nilsfjp.ideophonearena.model.Ideophone;
import io.github.nilsfjp.ideophonearena.model.PlayerAnswer;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import io.github.nilsfjp.ideophonearena.repository.ArenaRoundRepository;
import io.github.nilsfjp.ideophonearena.repository.IdeophoneGuessStatsProjection;
import io.github.nilsfjp.ideophonearena.repository.IdeophoneRatingStatsProjection;
import io.github.nilsfjp.ideophonearena.repository.IdeophoneRepository;
import io.github.nilsfjp.ideophonearena.repository.ModalityRatingDistributionProjection;
import io.github.nilsfjp.ideophonearena.repository.PlayerAnswerRepository;
import io.github.nilsfjp.ideophonearena.repository.RatingRepository;
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
    private final IdeophoneRepository ideophoneRepository;
    private final ArenaRoundRepository arenaRoundRepository;
    private final RoundShuffler roundShuffler;
    private final PositionBiasCalculator positionBiasCalculator;
    private final ResearchMapper researchMapper;

    public ResearchService(PlayerAnswerRepository playerAnswerRepository, RatingRepository ratingRepository,
            IdeophoneRepository ideophoneRepository, ArenaRoundRepository arenaRoundRepository,
            RoundShuffler roundShuffler, PositionBiasCalculator positionBiasCalculator,
            ResearchMapper researchMapper) {
        this.playerAnswerRepository = playerAnswerRepository;
        this.ratingRepository = ratingRepository;
        this.ideophoneRepository = ideophoneRepository;
        this.arenaRoundRepository = arenaRoundRepository;
        this.roundShuffler = roundShuffler;
        this.positionBiasCalculator = positionBiasCalculator;
        this.researchMapper = researchMapper;
    }

    // Guess accuracy (from player_answers) and mean rating (from ratings) are
    // independent aggregates; a single join across both would form a cartesian
    // product and inflate the correct-answer sum. So each is queried separately
    // and merged on the ideophone id, mirroring AdminStatsService.
    @Transactional(readOnly = true)
    public List<DivergenceResponse> getDivergence() {
        Map<Long, IdeophoneGuessStatsProjection> guessStats = new LinkedHashMap<>();
        for (IdeophoneGuessStatsProjection row : playerAnswerRepository.aggregateGuessStatsByIdeophone()) {
            guessStats.put(row.getIdeophoneId(), row);
        }

        Map<Long, IdeophoneRatingStatsProjection> ratingStats = new LinkedHashMap<>();
        for (IdeophoneRatingStatsProjection row : ratingRepository.aggregateRatingStatsByIdeophone()) {
            ratingStats.put(row.getIdeophoneId(), row);
        }

        // One row per ideophone that has at least one guess or one rating,
        // ordered by id for a deterministic response.
        TreeSet<Long> ideophoneIds = new TreeSet<>();
        ideophoneIds.addAll(guessStats.keySet());
        ideophoneIds.addAll(ratingStats.keySet());

        Map<Long, Ideophone> ideophones = new LinkedHashMap<>();
        for (Ideophone ideophone : ideophoneRepository.findAllById(ideophoneIds)) {
            ideophones.put(ideophone.getId(), ideophone);
        }

        List<DivergenceResponse> divergence = new ArrayList<>();
        for (Long ideophoneId : ideophoneIds) {
            Ideophone ideophone = ideophones.get(ideophoneId);
            if (ideophone == null) {
                continue;
            }
            IdeophoneGuessStatsProjection guess = guessStats.get(ideophoneId);
            long guessCount = guess == null ? 0L : valueOrZero(guess.getGuesses());
            long correct = guess == null ? 0L : valueOrZero(guess.getCorrect());

            IdeophoneRatingStatsProjection rating = ratingStats.get(ideophoneId);
            long ratingCount = rating == null ? 0L : valueOrZero(rating.getRatingCount());
            Double meanRating = rating == null ? null : rating.getMeanRating();

            divergence.add(researchMapper.toDivergenceResponse(ideophone, guessCount, correct, ratingCount,
                    meanRating));
        }
        return divergence;
    }

    // Rainclouds need per-value counts, not means. Each modality that has any
    // ratings gets a dense 1-7 grid (zero-filled by the mapper) plus its total
    // n; modalities with no ratings are omitted. NIL-68 later re-points this to
    // word-grain without changing the response shape.
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
        // The scored-rounds list is identical across sessions of the same
        // (condition, difficulty), so cache it by that key; but derive the
        // presentation per session because each shuffle seed differs.
        Map<String, List<ArenaRound>> roundsByGroup = new HashMap<>();
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
            Map<Long, DerivedRound> derivedByRoundId =
                    derivedBySession.computeIfAbsent(session.getId(), unused -> deriveByRoundId(session, roundsByGroup));

            DerivedRound derived = derivedByRoundId.get(answer.getRound().getId());
            if (derived == null) {
                // The answer's round is not in the session's derived scored set
                // (data drift). Skip rather than silently miscount.
                skipped++;
                continue;
            }

            long selectedId = answer.getSelectedIdeophone().getId();
            long targetId = answer.getTargetIdeophone().getId();
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
            log.warn("position-bias: skipped {} answer(s) whose round was absent from the derived scored set", skipped);
        }

        PositionBiasCalculator.SignalDetection sdt =
                positionBiasCalculator.signalDetection(hits, signalTrials, falseAlarms, noiseTrials);
        return researchMapper.toPositionBiasResponse(n, leftPickCount, sdt.getDPrime(), sdt.getCriterion(),
                targetTopN, targetTopCorrect, targetBottomN, targetBottomCorrect);
    }

    private Map<Long, DerivedRound> deriveByRoundId(GameSession session, Map<String, List<ArenaRound>> roundsByGroup) {
        String groupKey = session.getConditionName().name() + "#" + session.getDifficultyLevel();
        List<ArenaRound> rounds = roundsByGroup.computeIfAbsent(groupKey, unused ->
                arenaRoundRepository.findByConditionNameAndDifficultyLevelAndPracticeFalseOrderByIdAsc(
                        session.getConditionName(), session.getDifficultyLevel()));
        Map<Long, DerivedRound> byRoundId = new HashMap<>();
        for (DerivedRound derived : roundShuffler.deriveScoredRounds(session.getShuffleSeed(), rounds)) {
            byRoundId.put(derived.getRound().getId(), derived);
        }
        return byRoundId;
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
