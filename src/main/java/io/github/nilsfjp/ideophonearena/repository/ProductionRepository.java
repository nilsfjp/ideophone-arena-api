package io.github.nilsfjp.ideophonearena.repository;

import io.github.nilsfjp.ideophonearena.model.Production;
import io.github.nilsfjp.ideophonearena.model.Word;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductionRepository extends JpaRepository<Production, Long> {

    boolean existsByUserIdAndWordId(Long userId, Long wordId);

    long countByUserId(Long userId);

    // created_at is a TIMESTAMP with second resolution, so two productions minted in the
    // same second tie. The descending id breaks the tie by insertion order, which is what
    // "most recent first" means.
    @EntityGraph(attributePaths = "word")
    Page<Production> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    // The next prompt within one modality: the lowest-id scored word the caller has not
    // produced yet. "Scored inventory" is a trial fact (ADR-3), so membership of at least
    // one non-practice trial is the predicate -- not a stimulus_file prefix, which would
    // also admit seeded-but-untrialed (dark) inventory. Call with PageRequest.of(0, 1).
    @Query("""
            select word from Word word
            where word.modality = :modality
              and exists (
                  select 1 from Trial trial
                  join trial.pairing pairing
                  where trial.practice = false
                    and (pairing.wordA = word or pairing.wordB = word)
              )
              and not exists (
                  select 1 from Production production
                  where production.user.id = :userId and production.word = word
              )
            order by word.id asc
            """)
    List<Word> findNextUnproducedByModality(@Param("userId") Long userId,
            @Param("modality") Modality modality, Pageable pageable);

    // The producible universe: findNextUnproducedByModality's predicate minus the caller's
    // not-exists clause, widened from one modality to the whole cycle. It is the {n} in Word
    // Mint's "word {i} of {n}", so it must count exactly what the cycle can ever serve --
    // hence the modality fence. Modality also has TACTILE and MOTION, which the cycle never
    // visits; an unfenced count would over-report the day one of them is trialed, and {i}
    // could never reach {n}. Trial membership stays an exists subquery, not a join: a word
    // sits in many non-practice trials and a join would count it once per trial.
    @Query("""
            select count(word) from Word word
            where word.modality in :modalities
              and exists (
                  select 1 from Trial trial
                  join trial.pairing pairing
                  where trial.practice = false
                    and (pairing.wordA = word or pairing.wordB = word)
              )
            """)
    long countProducible(@Param("modalities") Collection<Modality> modalities);

    // Mean similarity per word (triangulation production side), word-keyed (ADR-0).
    // Carries the Rider A username fences for symmetry with the guess and rating
    // aggregates: browser_loop_* automation accounts are live-relevant, and thesis_p%
    // can never hold a production (registration rejects the prefix). No gameMode fence --
    // a production has no game mode, only an optional session for provenance.
    @Query("""
            select
                word.id as ideophoneId,
                count(production.id) as productionCount,
                avg(production.similarityScore) as meanProductionScore
            from Production production
            join production.word word
            where production.user.username not like 'browser!_loop!_%' escape '!'
              and production.user.username not like 'thesis!_p%' escape '!'
            group by word.id
            """)
    List<IdeophoneProductionStatsProjection> aggregateProductionStatsByWord();
}
