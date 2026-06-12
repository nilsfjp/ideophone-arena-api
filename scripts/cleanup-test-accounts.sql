-- Removes locally registered browser_loop_* automation accounts (created by
-- browser-based test loops, not by the seed) together with their sessions and
-- answers, so they stop appearing on the leaderboard. Idempotent: running it
-- again deletes nothing. FK order: player_answers -> game_sessions -> app_users.
--
-- Usage (Windows mysql.exe from WSL, see docs/demo-runbook.md):
--   mysql.exe -u root -p"$PW" --default-character-set=utf8mb4 < scripts/cleanup-test-accounts.sql

USE ideophone_arena;

DELETE player_answers
FROM player_answers
         JOIN game_sessions ON player_answers.session_id = game_sessions.id
         JOIN app_users ON game_sessions.user_id = app_users.id
WHERE app_users.username LIKE 'browser\_loop\_%';

DELETE game_sessions
FROM game_sessions
         JOIN app_users ON game_sessions.user_id = app_users.id
WHERE app_users.username LIKE 'browser\_loop\_%';

DELETE
FROM app_users
WHERE username LIKE 'browser\_loop\_%';
