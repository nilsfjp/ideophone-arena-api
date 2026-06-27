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
        name = "ratings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "ideophone_id"})
)
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ideophone_id", nullable = false)
    private Ideophone ideophone;

    // Provenance only: the session the rating was made during, if any. Ratings
    // are keyed by (user, ideophone), so session_id stays nullable.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private GameSession session;

    @Column(name = "rating", nullable = false)
    private short rating;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @CreationTimestamp
    @Column(name = "rated_at", nullable = false, updatable = false)
    private Instant ratedAt;

    protected Rating() {
    }

    public Rating(AppUser user, Ideophone ideophone, GameSession session, short rating, Integer responseTimeMs) {
        this.user = user;
        this.ideophone = ideophone;
        this.session = session;
        this.rating = rating;
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

    public Ideophone getIdeophone() {
        return ideophone;
    }

    public void setIdeophone(Ideophone ideophone) {
        this.ideophone = ideophone;
    }

    public GameSession getSession() {
        return session;
    }

    public void setSession(GameSession session) {
        this.session = session;
    }

    public short getRating() {
        return rating;
    }

    public void setRating(short rating) {
        this.rating = rating;
    }

    public Integer getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Integer responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public Instant getRatedAt() {
        return ratedAt;
    }
}
