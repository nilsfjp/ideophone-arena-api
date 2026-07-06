package io.github.nilsfjp.ideophonearena.repository;

import io.github.nilsfjp.ideophonearena.model.Trial;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrialRepository extends JpaRepository<Trial, Long> {

    long countByPracticeFalse();

    // Trials are condition-free (ADR-3): every session's scored base list is the
    // same 30 trials ordered by id. The pairing's two words are fetched so the
    // shuffle can compare word ids and the mapper can render without lazy loads.
    @EntityGraph(attributePaths = {"pairing", "pairing.wordA", "pairing.wordB"})
    List<Trial> findByPracticeFalseOrderByIdAsc();

    @EntityGraph(attributePaths = {"pairing", "pairing.wordA", "pairing.wordB"})
    List<Trial> findByPracticeTrueOrderByIdAsc();

    @EntityGraph(attributePaths = {"pairing", "pairing.wordA", "pairing.wordB", "correctWord"})
    @Query("select trial from Trial trial where trial.id = :id")
    Optional<Trial> findByIdWithPairingWords(@Param("id") Long id);
}
