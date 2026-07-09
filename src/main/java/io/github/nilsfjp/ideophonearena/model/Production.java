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

// The production measure (NIL-62): one invented word per user per word.
@Entity
@Table(
        name = "productions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "word_id"})
)
public class Production {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    // Word-keyed (ADR-0): UNIQUE(user_id, word_id) is what makes "your first
    // instinct is the datum" a database fact rather than a UI convention.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_id", nullable = false)
    private Word word;

    // Provenance only, like ratings: which session the player was in, if any.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private GameSession session;

    // The durable facts. raw_input plus scorer_version let any past or future
    // scorer recompute the features, which is why no breakdown is stored.
    @Column(name = "raw_input", nullable = false, length = 24)
    private String rawInput;

    @Column(name = "normalized_form", nullable = false, length = 40)
    private String normalizedForm;

    @Column(name = "similarity_score", nullable = false)
    private short similarityScore;

    @Column(name = "scorer_version", nullable = false)
    private short scorerVersion;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Production() {
    }

    public Production(AppUser user, Word word, GameSession session, String rawInput, String normalizedForm,
            short similarityScore, short scorerVersion, Integer responseTimeMs) {
        this.user = user;
        this.word = word;
        this.session = session;
        this.rawInput = rawInput;
        this.normalizedForm = normalizedForm;
        this.similarityScore = similarityScore;
        this.scorerVersion = scorerVersion;
        this.responseTimeMs = responseTimeMs;
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public Word getWord() {
        return word;
    }

    public void setWord(Word word) {
        this.word = word;
    }

    public GameSession getSession() {
        return session;
    }

    public void setSession(GameSession session) {
        this.session = session;
    }

    public String getRawInput() {
        return rawInput;
    }

    public String getNormalizedForm() {
        return normalizedForm;
    }

    public short getSimilarityScore() {
        return similarityScore;
    }

    public short getScorerVersion() {
        return scorerVersion;
    }

    public Integer getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Integer responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
