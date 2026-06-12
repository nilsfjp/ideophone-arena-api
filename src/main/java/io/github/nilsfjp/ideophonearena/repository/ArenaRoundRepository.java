package io.github.nilsfjp.ideophonearena.repository;

import io.github.nilsfjp.ideophonearena.model.ArenaRound;
import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArenaRoundRepository extends JpaRepository<ArenaRound, Long> {

    long countByConditionNameAndDifficultyLevel(ConditionName conditionName, int difficultyLevel);

    long countByConditionNameAndDifficultyLevelAndPracticeFalse(ConditionName conditionName, int difficultyLevel);

    @EntityGraph(attributePaths = {"leftIdeophone", "rightIdeophone"})
    List<ArenaRound> findByConditionNameAndDifficultyLevelAndPracticeFalseOrderByIdAsc(
            ConditionName conditionName,
            int difficultyLevel
    );

    @EntityGraph(attributePaths = {"leftIdeophone", "rightIdeophone"})
    List<ArenaRound> findByConditionNameAndDifficultyLevelAndPracticeTrueOrderByIdAsc(
            ConditionName conditionName,
            int difficultyLevel
    );

    @EntityGraph(attributePaths = {"leftIdeophone", "rightIdeophone", "correctIdeophone"})
    @Query("select round from ArenaRound round where round.id = :id")
    Optional<ArenaRound> findByIdWithIdeophones(@Param("id") Long id);
}
