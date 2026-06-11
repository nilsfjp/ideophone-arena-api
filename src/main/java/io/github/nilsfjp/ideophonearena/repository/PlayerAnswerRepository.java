package io.github.nilsfjp.ideophonearena.repository;

import io.github.nilsfjp.ideophonearena.model.PlayerAnswer;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerAnswerRepository extends JpaRepository<PlayerAnswer, Long> {

    boolean existsBySessionIdAndRoundId(Long sessionId, Long roundId);

    @EntityGraph(attributePaths = {"round", "round.correctIdeophone", "selectedIdeophone"})
    List<PlayerAnswer> findBySessionId(Long sessionId);

    @EntityGraph(attributePaths = {"round", "round.correctIdeophone", "selectedIdeophone"})
    List<PlayerAnswer> findBySessionIdOrderByAnsweredAtAsc(Long sessionId);

    @EntityGraph(attributePaths = {"session", "round", "round.correctIdeophone", "selectedIdeophone"})
    List<PlayerAnswer> findBySessionUserIdOrderByAnsweredAtDesc(Long userId, Pageable pageable);

    @Query("select answer.round.id from PlayerAnswer answer where answer.session.id = :sessionId")
    List<Long> findAnsweredRoundIdsBySessionId(@Param("sessionId") Long sessionId);

    long countBySessionId(Long sessionId);

    long countBySessionIdAndCorrectTrue(Long sessionId);

    // Callers must pass an unsorted Pageable: a Pageable sort would be appended
    // after the aggregate order by. The username tiebreak keeps page boundaries
    // deterministic.
    @Query(value = """
            select
                user.id as userId,
                user.username as username,
                count(answer.id) as totalAnswers,
                sum(case when answer.correct = true then 1 else 0 end) as correctAnswers,
                avg(answer.responseTimeMs) as averageResponseTimeMs
            from PlayerAnswer answer
            join answer.session session
            join session.user user
            group by user.id, user.username
            order by
                sum(case when answer.correct = true then 1 else 0 end) desc,
                count(answer.id) desc,
                avg(answer.responseTimeMs) asc,
                user.username asc
            """,
            countQuery = """
            select count(distinct session.user.id)
            from PlayerAnswer answer
            join answer.session session
            """)
    Page<LeaderboardEntryProjection> findLeaderboard(Pageable pageable);

    @Query("""
            select
                session.conditionName as conditionName,
                count(answer.id) as answers,
                sum(case when answer.correct = true then 1 else 0 end) as correct
            from PlayerAnswer answer
            join answer.session session
            group by session.conditionName
            order by session.conditionName
            """)
    List<ConditionAnswerStatsProjection> aggregateAnswersByCondition();

    @Query("""
            select
                ideophone.modality as modality,
                count(answer.id) as answers,
                sum(case when answer.correct = true then 1 else 0 end) as correct
            from PlayerAnswer answer
            join answer.round round
            join round.correctIdeophone ideophone
            where ideophone.modality is not null
            group by ideophone.modality
            order by ideophone.modality
            """)
    List<ModalityAnswerStatsProjection> aggregateAnswersByModality();
}
