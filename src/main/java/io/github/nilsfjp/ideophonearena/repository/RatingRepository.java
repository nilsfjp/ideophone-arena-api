package io.github.nilsfjp.ideophonearena.repository;

import io.github.nilsfjp.ideophonearena.model.Rating;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    boolean existsByUserIdAndWordId(Long userId, Long wordId);

    // rated_at is a TIMESTAMP with second resolution, so two ratings minted in the same
    // second tie. The descending id breaks the tie by insertion order, which is what
    // "most recent first" means (mirrors ProductionRepository).
    @EntityGraph(attributePaths = "word")
    Page<Rating> findByUserIdOrderByRatedAtDescIdDesc(Long userId, Pageable pageable);

    // Mean rating per word (divergence rating side), word-keyed (ADR-0). Rider A
    // excludes browser_loop_* automation accounts and the thesis_p% ingestion
    // cohort (NIL-54) -- no response-shape change.
    @Query("""
            select
                word.id as ideophoneId,
                count(rating.id) as ratingCount,
                avg(rating.rating) as meanRating
            from Rating rating
            join rating.word word
            where rating.user.username not like 'browser!_loop!_%' escape '!'
              and rating.user.username not like 'thesis!_p%' escape '!'
            group by word.id
            """)
    List<IdeophoneRatingStatsProjection> aggregateRatingStatsByWord();

    // Thesis-cohort counterpart (NIL-54 Observatory thesis layer): predicate
    // inverted to INCLUDE only the thesis_p% cohort, so the thesis divergence
    // endpoint carries the thesis mean iconicity rating per word.
    @Query("""
            select
                word.id as ideophoneId,
                count(rating.id) as ratingCount,
                avg(rating.rating) as meanRating
            from Rating rating
            join rating.word word
            where rating.user.username like 'thesis!_p%' escape '!'
            group by word.id
            """)
    List<IdeophoneRatingStatsProjection> aggregateThesisRatingStatsByWord();

    // Population distribution of the 1-7 rating values per modality (per-value
    // counts, not means), for the Observatory raincloud panels. Null-modality
    // words are excluded -- they cannot belong to a modality panel. Word-keyed
    // (ADR-0); Rider A excludes browser_loop_* automation accounts and the
    // thesis_p% ingestion cohort (NIL-54).
    @Query("""
            select
                word.modality as modality,
                rating.rating as ratingValue,
                count(rating.id) as count
            from Rating rating
            join rating.word word
            where word.modality is not null
              and rating.user.username not like 'browser!_loop!_%' escape '!'
              and rating.user.username not like 'thesis!_p%' escape '!'
            group by word.modality, rating.rating
            """)
    List<ModalityRatingDistributionProjection> aggregateRatingDistribution();
}
