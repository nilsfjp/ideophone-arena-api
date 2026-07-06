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
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "trial_id"})
)
public class PlayerAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trial_id", nullable = false)
    private Trial trial;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "selected_word_id", nullable = false)
    private Word selectedWord;

    // The seed-derived target of this trial in this session, stored at answer
    // time so analytics can aggregate per actually-served target -- including the
    // complementary targets the thesis never measured (trials.correct_word_id
    // only documents the thesis fixed target).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_word_id", nullable = false)
    private Word targetWord;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @CreationTimestamp
    @Column(name = "answered_at", nullable = false, updatable = false)
    private Instant answeredAt;

    protected PlayerAnswer() {
    }

    public PlayerAnswer(GameSession session, Trial trial, Word selectedWord, Word targetWord,
            Integer responseTimeMs, boolean correct) {
        this.session = session;
        this.trial = trial;
        this.selectedWord = selectedWord;
        this.targetWord = targetWord;
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

    public Trial getTrial() {
        return trial;
    }

    public void setTrial(Trial trial) {
        this.trial = trial;
    }

    public Word getSelectedWord() {
        return selectedWord;
    }

    public void setSelectedWord(Word selectedWord) {
        this.selectedWord = selectedWord;
    }

    public Word getTargetWord() {
        return targetWord;
    }

    public void setTargetWord(Word targetWord) {
        this.targetWord = targetWord;
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
