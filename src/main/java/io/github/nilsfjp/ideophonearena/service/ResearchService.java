package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.dto.DivergenceResponse;
import io.github.nilsfjp.ideophonearena.mapper.ResearchMapper;
import io.github.nilsfjp.ideophonearena.model.Ideophone;
import io.github.nilsfjp.ideophonearena.repository.IdeophoneGuessStatsProjection;
import io.github.nilsfjp.ideophonearena.repository.IdeophoneRatingStatsProjection;
import io.github.nilsfjp.ideophonearena.repository.IdeophoneRepository;
import io.github.nilsfjp.ideophonearena.repository.PlayerAnswerRepository;
import io.github.nilsfjp.ideophonearena.repository.RatingRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResearchService {

    private final PlayerAnswerRepository playerAnswerRepository;
    private final RatingRepository ratingRepository;
    private final IdeophoneRepository ideophoneRepository;
    private final ResearchMapper researchMapper;

    public ResearchService(PlayerAnswerRepository playerAnswerRepository, RatingRepository ratingRepository,
            IdeophoneRepository ideophoneRepository, ResearchMapper researchMapper) {
        this.playerAnswerRepository = playerAnswerRepository;
        this.ratingRepository = ratingRepository;
        this.ideophoneRepository = ideophoneRepository;
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

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
