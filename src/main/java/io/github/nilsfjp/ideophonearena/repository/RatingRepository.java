package io.github.nilsfjp.ideophonearena.repository;

import io.github.nilsfjp.ideophonearena.model.Rating;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    boolean existsByUserIdAndIdeophoneId(Long userId, Long ideophoneId);

    @EntityGraph(attributePaths = "ideophone")
    Page<Rating> findByUserIdOrderByRatedAtDesc(Long userId, Pageable pageable);

    @Query("""
            select
                ideophone.id as ideophoneId,
                count(rating.id) as ratingCount,
                avg(rating.rating) as meanRating
            from Rating rating
            join rating.ideophone ideophone
            group by ideophone.id
            """)
    List<IdeophoneRatingStatsProjection> aggregateRatingStatsByIdeophone();
}
