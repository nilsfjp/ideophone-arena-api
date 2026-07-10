-- Removes locally registered automation and test-harness accounts (created by
-- browser-based test loops and by ./mvnw test, never by the seed) together with
-- their ratings, sessions, and answers, so they stop appearing on the leaderboard.
-- Idempotent: running it again deletes nothing. FK order:
-- ratings -> player_answers -> game_sessions -> app_users (ratings reference
-- both app_users and, for pre-27E rows, game_sessions).
--
-- Which accounts: every harness account is minted with a `System.nanoTime()` or
-- `Date.now()` suffix (browser_loop_1751... , practice_http_43217127168271, ...),
-- so a trailing run of >= 10 digits identifies them all -- across the ~50 prefixes
-- the JUnit HTTP tests use, without an enumeration that rots as tests are added.
-- The seeded and human accounts have no such suffix and survive: `arena_admin`,
-- the `thesis_p01`..`thesis_p36` ingestion cohort (only two digits), and any real
-- dev login. Verify before running:
--   SELECT username FROM app_users WHERE username NOT REGEXP '_[0-9]{10,}$';
--
-- Why it matters (NIL-85): the leaderboard ranks completed CHOOSING sessions by
-- raw correct count, and pre-NIL-85 harness runs left 47-answer sessions behind.
-- A session now serves 21 rounds, so those relics outrank any real player until
-- they are cleaned. They are all harness-owned; no real session exists.
--
-- Usage (Windows mysql.exe from WSL, see docs/demo-runbook.md):
--   mysql.exe -u root -p"$PW" --default-character-set=utf8mb4 < scripts/cleanup-test-accounts.sql

USE ideophone_arena;

DELETE ratings
FROM ratings
         JOIN app_users ON ratings.user_id = app_users.id
WHERE app_users.username REGEXP '_[0-9]{10,}$';

DELETE player_answers
FROM player_answers
         JOIN game_sessions ON player_answers.session_id = game_sessions.id
         JOIN app_users ON game_sessions.user_id = app_users.id
WHERE app_users.username REGEXP '_[0-9]{10,}$';

DELETE game_sessions
FROM game_sessions
         JOIN app_users ON game_sessions.user_id = app_users.id
WHERE app_users.username REGEXP '_[0-9]{10,}$';

DELETE
FROM app_users
WHERE username REGEXP '_[0-9]{10,}$';
