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

    @EntityGraph(attributePaths = {"round", "targetIdeophone", "selectedIdeophone"})
    List<PlayerAnswer> findBySessionId(Long sessionId);

    @EntityGraph(attributePaths = {"round", "targetIdeophone", "selectedIdeophone"})
    List<PlayerAnswer> findBySessionIdOrderByAnsweredAtAsc(Long sessionId);

    @EntityGraph(attributePaths = {"session", "round", "targetIdeophone", "selectedIdeophone"})
    List<PlayerAnswer> findBySessionUserIdOrderByAnsweredAtDesc(Long userId, Pageable pageable);

    @Query("select answer.round.id from PlayerAnswer answer where answer.session.id = :sessionId")
    List<Long> findAnsweredRoundIdsBySessionId(@Param("sessionId") Long sessionId);

    long countBySessionId(Long sessionId);

    long countBySessionIdAndCorrectTrue(Long sessionId);

    // One row per user: that user's best completed session (most correct
    // answers; ties broken by fewer answers = higher accuracy, then lower
    // session id so the row is unique). Callers must pass an unsorted Pageable:
    // a Pageable sort would be appended after the aggregate order by. The
    // username tiebreak keeps page boundaries deterministic.
    @Query(value = """
            select
                best.username as username,
                best.correctCount as bestSessionCorrect,
                best.answeredCount as bestSessionAnswered
            from (
                select
                    user.id as userId,
                    user.username as username,
                    session.id as sessionId,
                    count(answer.id) as answeredCount,
                    sum(case when answer.correct = true then 1 else 0 end) as correctCount
                from PlayerAnswer answer
                join answer.session session
                join session.user user
                where session.completedAt is not null
                group by session.id, user.id, user.username
            ) best
            where not exists (
                select 1
                from PlayerAnswer other
                join other.session otherSession
                where otherSession.user.id = best.userId
                  and otherSession.completedAt is not null
                  and otherSession.id <> best.sessionId
                group by otherSession.id
                having sum(case when other.correct = true then 1 else 0 end) > best.correctCount
                    or (sum(case when other.correct = true then 1 else 0 end) = best.correctCount
                        and count(other.id) < best.answeredCount)
                    or (sum(case when other.correct = true then 1 else 0 end) = best.correctCount
                        and count(other.id) = best.answeredCount
                        and otherSession.id < best.sessionId)
            )
            order by
                best.correctCount desc,
                best.answeredCount asc,
                best.username asc
            """,
            countQuery = """
            select count(distinct session.user.id)
            from GameSession session
            where session.completedAt is not null
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
            join answer.targetIdeophone ideophone
            where ideophone.modality is not null
            group by ideophone.modality
            order by ideophone.modality
            """)
    List<ModalityAnswerStatsProjection> aggregateAnswersByModality();
}
