package io.github.nilsfjp.ideophonearena.repository;

import io.github.nilsfjp.ideophonearena.model.GameSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<GameSession> findBySessionUuid(String sessionUuid);

    @EntityGraph(attributePaths = "user")
    List<GameSession> findByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    List<GameSession> findByUserIdOrderByStartedAtDesc(Long userId);
}
