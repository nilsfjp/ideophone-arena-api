package io.github.nilsfjp.ideophonearena.repository;

import io.github.nilsfjp.ideophonearena.model.GameSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<GameSession> findBySessionUuid(String sessionUuid);

    @EntityGraph(attributePaths = "user")
    List<GameSession> findByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    List<GameSession> findByUserIdOrderByStartedAtDesc(Long userId);

    long countByCompletedAtNotNull();

    @Query("""
            select
                session.conditionName as conditionName,
                count(session.id) as sessions
            from GameSession session
            group by session.conditionName
            order by session.conditionName
            """)
    List<ConditionSessionCountProjection> countSessionsByCondition();
}
