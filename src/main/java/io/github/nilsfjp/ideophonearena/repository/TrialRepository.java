package io.github.nilsfjp.ideophonearena.repository;

import io.github.nilsfjp.ideophonearena.model.Trial;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrialRepository extends JpaRepository<Trial, Long> {

    long countByPracticeFalse();

    // The Meaning Match (CHOOSING) scored base list: every non-practice, non-HAPTIC trial
    // ordered by id -- exactly the 47 A/V/I trials. HAPTIC trials exist (NIL-41 Touch floor)
    // but are served only through the Perception Ladder, so excluding them here keeps this
    // list byte-identical to what it was before the Touch go-live, preserving the frozen
    // shuffle derivation for every existing session. Both consumers (game serving +
    // position-bias replay) share this query so neither can drift.
    @EntityGraph(attributePaths = {"pairing", "pairing.wordA", "pairing.wordB"})
    @Query("""
            select trial from Trial trial
            where trial.practice = false
              and trial.pairing.modality <> io.github.nilsfjp.ideophonearena.model.enums.Modality.HAPTIC
            order by trial.id asc
            """)
    List<Trial> findScoredChoosingTrials();

    // Perception Ladder floor serving: the scored trials for a floor's pair codes. The
    // caller reorders them by the floor's fixed easy->hard map (LadderFloors), so the
    // query order is unimportant.
    @EntityGraph(attributePaths = {"pairing", "pairing.wordA", "pairing.wordB"})
    @Query("select trial from Trial trial where trial.practice = false and trial.pairing.pairCode in :pairCodes")
    List<Trial> findScoredTrialsByPairCodes(@Param("pairCodes") Collection<String> pairCodes);

    @EntityGraph(attributePaths = {"pairing", "pairing.wordA", "pairing.wordB"})
    List<Trial> findByPracticeTrueOrderByIdAsc();

    @EntityGraph(attributePaths = {"pairing", "pairing.wordA", "pairing.wordB", "correctWord"})
    @Query("select trial from Trial trial where trial.id = :id")
    Optional<Trial> findByIdWithPairingWords(@Param("id") Long id);
}
