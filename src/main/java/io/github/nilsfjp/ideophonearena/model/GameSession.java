package io.github.nilsfjp.ideophonearena.model;

import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;
import io.github.nilsfjp.ideophonearena.model.enums.GameMode;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "game_sessions")
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_uuid", nullable = false, unique = true, length = 36)
    private String sessionUuid = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    // Locked experiment invariant: difficulty is fixed at 1 (no longer client-supplied,
    // A3). The column stays so the frozen schema and thesis seed are untouched.
    @Column(name = "difficulty_level", nullable = false)
    private int difficultyLevel = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_name", nullable = false, length = 50)
    private ConditionName conditionName;

    // M1 (ADR-2): the session's play mode. Defaults to CHOOSING so legacy/core sessions
    // are unchanged; LADDER sessions also carry a ladderFloor (the served Perception
    // Ladder floor, a Modality). ladderFloor is null for every non-LADDER mode.
    @Enumerated(EnumType.STRING)
    @Column(name = "game_mode", nullable = false, length = 30)
    private GameMode gameMode = GameMode.CHOOSING;

    @Enumerated(EnumType.STRING)
    @Column(name = "ladder_floor", length = 30)
    private Modality ladderFloor;

    @Column(name = "include_practice", nullable = false)
    private boolean includePractice;

    @Column(name = "practice_answered", nullable = false)
    private int practiceAnswered;

    // Seeds the deterministic per-session shuffle (round order, target
    // identity, target side, meaning order). Generated once at session
    // creation and never exposed in player-facing DTOs.
    @Column(name = "shuffle_seed", nullable = false)
    private long shuffleSeed;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected GameSession() {
    }

    // CHOOSING (Meaning Match) session -- the common case.
    public GameSession(AppUser user, ConditionName conditionName, boolean includePractice, long shuffleSeed) {
        this(user, conditionName, GameMode.CHOOSING, null, includePractice, shuffleSeed);
    }

    public GameSession(AppUser user, ConditionName conditionName, GameMode gameMode, Modality ladderFloor,
            boolean includePractice, long shuffleSeed) {
        this.user = user;
        this.conditionName = conditionName;
        this.gameMode = gameMode;
        this.ladderFloor = ladderFloor;
        this.includePractice = includePractice;
        this.shuffleSeed = shuffleSeed;
    }

    public void complete() {
        this.completedAt = Instant.now();
    }

    public void recordPracticeAnswer() {
        this.practiceAnswered++;
    }

    public Long getId() {
        return id;
    }

    public String getSessionUuid() {
        return sessionUuid;
    }

    public void setSessionUuid(String sessionUuid) {
        this.sessionUuid = sessionUuid;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public ConditionName getConditionName() {
        return conditionName;
    }

    public void setConditionName(ConditionName conditionName) {
        this.conditionName = conditionName;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public Modality getLadderFloor() {
        return ladderFloor;
    }

    public void setLadderFloor(Modality ladderFloor) {
        this.ladderFloor = ladderFloor;
    }

    public boolean isIncludePractice() {
        return includePractice;
    }

    public void setIncludePractice(boolean includePractice) {
        this.includePractice = includePractice;
    }

    public int getPracticeAnswered() {
        return practiceAnswered;
    }

    public void setPracticeAnswered(int practiceAnswered) {
        this.practiceAnswered = practiceAnswered;
    }

    public long getShuffleSeed() {
        return shuffleSeed;
    }

    public void setShuffleSeed(long shuffleSeed) {
        this.shuffleSeed = shuffleSeed;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
