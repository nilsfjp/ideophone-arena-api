package io.github.nilsfjp.ideophonearena.repository;

import io.github.nilsfjp.ideophonearena.model.PlayerAnswer;
import java.util.List;
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

    @Query("select count(answer.id) from PlayerAnswer answer where answer.session.user.id = :userId")
    long countBySessionUserId(@Param("userId") Long userId);

    @Query("""
            select count(answer.id)
            from PlayerAnswer answer
            where answer.session.user.id = :userId
                and answer.correct = true
            """)
    long countBySessionUserIdAndCorrectTrue(@Param("userId") Long userId);

    @Query("""
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
                avg(answer.responseTimeMs) asc
            """)
    List<LeaderboardEntryProjection> findLeaderboard(Pageable pageable);
}
