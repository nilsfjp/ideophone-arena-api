package io.github.nilsfjp.ideophonearena.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
        name = "player_answers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "round_id"})
)
public class PlayerAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id", nullable = false)
    private ArenaRound round;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "selected_ideophone_id", nullable = false)
    private Ideophone selectedIdeophone;

    // The seed-derived target of this round in this session, stored at
    // answer time so analytics can aggregate per actually-served target
    // (arena_rounds.correct_ideophone_id only documents the thesis target).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_ideophone_id", nullable = false)
    private Ideophone targetIdeophone;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @CreationTimestamp
    @Column(name = "answered_at", nullable = false, updatable = false)
    private Instant answeredAt;

    protected PlayerAnswer() {
    }

    public PlayerAnswer(GameSession session, ArenaRound round, Ideophone selectedIdeophone, Ideophone targetIdeophone,
            Integer responseTimeMs, boolean correct) {
        this.session = session;
        this.round = round;
        this.selectedIdeophone = selectedIdeophone;
        this.targetIdeophone = targetIdeophone;
        this.responseTimeMs = responseTimeMs;
        this.correct = correct;
    }

    public Long getId() {
        return id;
    }

    public GameSession getSession() {
        return session;
    }

    public void setSession(GameSession session) {
        this.session = session;
    }

    public ArenaRound getRound() {
        return round;
    }

    public void setRound(ArenaRound round) {
        this.round = round;
    }

    public Ideophone getSelectedIdeophone() {
        return selectedIdeophone;
    }

    public void setSelectedIdeophone(Ideophone selectedIdeophone) {
        this.selectedIdeophone = selectedIdeophone;
    }

    public Ideophone getTargetIdeophone() {
        return targetIdeophone;
    }

    public void setTargetIdeophone(Ideophone targetIdeophone) {
        this.targetIdeophone = targetIdeophone;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public Integer getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Integer responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }
}
