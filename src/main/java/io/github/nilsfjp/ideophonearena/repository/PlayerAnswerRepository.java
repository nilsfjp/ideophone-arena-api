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

    boolean existsBySessionIdAndTrialId(Long sessionId, Long trialId);

    @EntityGraph(attributePaths = {"trial", "targetWord", "selectedWord"})
    List<PlayerAnswer> findBySessionId(Long sessionId);

    @EntityGraph(attributePaths = {"trial", "targetWord", "selectedWord"})
    List<PlayerAnswer> findBySessionIdOrderByAnsweredAtAsc(Long sessionId);

    @EntityGraph(attributePaths = {"session", "trial", "targetWord", "selectedWord"})
    List<PlayerAnswer> findBySessionUserIdOrderByAnsweredAtDesc(Long userId, Pageable pageable);

    @Query("select answer.trial.id from PlayerAnswer answer where answer.session.id = :sessionId")
    List<Long> findAnsweredTrialIdsBySessionId(@Param("sessionId") Long sessionId);

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
                word.modality as modality,
                count(answer.id) as answers,
                sum(case when answer.correct = true then 1 else 0 end) as correct
            from PlayerAnswer answer
            join answer.targetWord word
            where word.modality is not null
            group by word.modality
            order by word.modality
            """)
    List<ModalityAnswerStatsProjection> aggregateAnswersByModality();

    // The Rating Lab pool, word-keyed (ADR-0 grain-heal): every word the user met
    // through answered rounds -- both pairing members, since feedback reveals the
    // full word-to-meaning mapping -- minus words the user has already rated.
    // Grouping by the word (not a condition row) closes the cross-condition
    // double-offer: a word met under any condition appears once. Practice answers
    // are never persisted, so the practice predicate is defensive. Ordered by
    // first encounter (word-id tiebreak) so the pool is identical across devices;
    // callers must pass an unsorted Pageable (see findLeaderboard).
    @Query(value = """
            select
                word.id as ideophoneId,
                word.canonicalForm as canonicalForm,
                word.romaji as romaji,
                word.stimulusFile as stimulusFile,
                word.modality as modality,
                word.gloss as gloss,
                min(answer.answeredAt) as firstAnsweredAt
            from PlayerAnswer answer
            join answer.trial trial
            join trial.pairing pairing
            join Word word
                on word = pairing.wordA or word = pairing.wordB
            where answer.session.user.id = :userId
              and trial.practice = false
              and not exists (
                  select 1
                  from Rating rating
                  where rating.user.id = :userId
                    and rating.word = word
              )
            group by word.id, word.canonicalForm, word.romaji,
                word.stimulusFile, word.modality, word.gloss
            order by min(answer.answeredAt) asc, word.id asc
            """,
            countQuery = """
            select count(distinct word.id)
            from PlayerAnswer answer
            join answer.trial trial
            join trial.pairing pairing
            join Word word
                on word = pairing.wordA or word = pairing.wordB
            where answer.session.user.id = :userId
              and trial.practice = false
              and not exists (
                  select 1
                  from Rating rating
                  where rating.user.id = :userId
                    and rating.word = word
              )
            """)
    Page<RatableWordProjection> findRatableWordsByUserId(@Param("userId") Long userId, Pageable pageable);

    // Guess accuracy per word: grouped by the round's derived target
    // (target_word_id), so this measures how guessable each word's meaning is.
    // Divergence heals to one row per word (ADR-0). Rider A excludes practice
    // trials (defensive) and browser_loop_* automation accounts (the same rows
    // scripts/cleanup-test-accounts.sql deletes) -- no response-shape change.
    @Query("""
            select
                word.id as ideophoneId,
                count(answer.id) as guesses,
                sum(case when answer.correct = true then 1 else 0 end) as correct
            from PlayerAnswer answer
            join answer.targetWord word
            where answer.trial.practice = false
              and answer.session.user.username not like 'browser!_loop!_%' escape '!'
            group by word.id
            """)
    List<IdeophoneGuessStatsProjection> aggregateGuessStatsByWord();

    // Every scored, non-automation answer with the graph the position-bias
    // aggregate needs to replay each session's shuffle: the session (seed), the
    // trial (matched by id against the derived presentation), and the
    // selected/target words (matched by id to a side). The shuffle replay itself
    // (RoundShuffler) is byte-for-byte unchanged; only these joins move to word
    // grain, and Rider A adds the browser_loop_* exclusion.
    @Query("""
            select answer
            from PlayerAnswer answer
            join fetch answer.session
            join fetch answer.trial
            join fetch answer.selectedWord
            join fetch answer.targetWord
            where answer.trial.practice = false
              and answer.session.user.username not like 'browser!_loop!_%' escape '!'
            """)
    List<PlayerAnswer> findScoredForPositionBias();
}
