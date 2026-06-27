package io.github.nilsfjp.ideophonearena.repository;

import io.github.nilsfjp.ideophonearena.model.Rating;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    boolean existsByUserIdAndIdeophoneId(Long userId, Long ideophoneId);

    @EntityGraph(attributePaths = "ideophone")
    List<Rating> findByUserIdOrderByRatedAtDesc(Long userId, Pageable pageable);
}
