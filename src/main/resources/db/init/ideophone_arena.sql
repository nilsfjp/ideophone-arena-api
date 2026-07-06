CREATE DATABASE IF NOT EXISTS ideophone_arena
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE ideophone_arena;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS ratings;
DROP TABLE IF EXISTS player_answers;
DROP TABLE IF EXISTS game_sessions;
DROP TABLE IF EXISTS trials;
DROP TABLE IF EXISTS pairings;
DROP TABLE IF EXISTS presentations;
DROP TABLE IF EXISTS words;
DROP TABLE IF EXISTS languages;
DROP TABLE IF EXISTS app_users;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE app_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL
        UNIQUE,
    email VARCHAR(255) NOT NULL
        UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ADR-1: language as a first-class entity; every v1 word backfills to jpn.
CREATE TABLE languages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    iso_code VARCHAR(8) NOT NULL
        UNIQUE,
    name VARCHAR(50) NOT NULL,
    family VARCHAR(50),
    player_note VARCHAR(255),

    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ADR-0: word identity as an entity. One row per word (68); the per-word
-- shared audio (invariant 2) lives here once, not duplicated per condition.
CREATE TABLE words (
    id BIGINT NOT NULL AUTO_INCREMENT,
    language_id BIGINT NOT NULL,
    romaji VARCHAR(100) NOT NULL,
    kana VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    canonical_form VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    canonical_script VARCHAR(20) NOT NULL,
    gloss VARCHAR(255) NOT NULL,
    modality VARCHAR(50),
    semantic_category VARCHAR(20),
    stimulus_file VARCHAR(100) NOT NULL,

    PRIMARY KEY (id),
    UNIQUE (language_id, romaji),

    CONSTRAINT fk_words_language
        FOREIGN KEY (language_id)
            REFERENCES languages (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ADR-0: the script manipulation, and nothing else. One row per word x
-- scripted condition (204); display_form is verbatim (invariant 1).
CREATE TABLE presentations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    word_id BIGINT NOT NULL,
    condition_name VARCHAR(50) NOT NULL,
    display_form VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    script_code VARCHAR(2) NOT NULL,

    PRIMARY KEY (id),
    UNIQUE (word_id, condition_name),

    CONSTRAINT fk_presentations_word
        FOREIGN KEY (word_id)
            REFERENCES words (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ADR-6: the durable pair-level inventory. Members are FKs to words; the
-- thesis backfill sets is_core, source, and per-pair thesis_accuracy.
CREATE TABLE pairings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    pair_code VARCHAR(20) NOT NULL
        UNIQUE,
    language_id BIGINT NOT NULL,
    word_a_id BIGINT NOT NULL,
    word_b_id BIGINT NOT NULL,
    modality VARCHAR(50),
    is_core BOOLEAN NOT NULL,
    source VARCHAR(30) NOT NULL,
    difficulty_prior VARCHAR(10),
    thesis_accuracy DECIMAL(5,4),
    foil_distance DECIMAL(6,4),
    signoff_ref VARCHAR(100),
    approved_at DATE,

    PRIMARY KEY (id),

    CONSTRAINT fk_pairings_language
        FOREIGN KEY (language_id)
            REFERENCES languages (id),

    CONSTRAINT fk_pairings_word_a
        FOREIGN KEY (word_a_id)
            REFERENCES words (id),

    CONSTRAINT fk_pairings_word_b
        FOREIGN KEY (word_b_id)
            REFERENCES words (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ADR-3: one trials table, condition collapsed (102 rounds -> 34 trials).
-- correct_word_id documents the thesis fixed target (unread for CHOOSING).
CREATE TABLE trials (
    id BIGINT NOT NULL AUTO_INCREMENT,
    round_type VARCHAR(20) NOT NULL DEFAULT 'CHOOSING',
    pairing_id BIGINT NOT NULL,
    correct_word_id BIGINT NULL,
    feature_axis VARCHAR(10) NULL,
    is_practice BOOLEAN NOT NULL DEFAULT FALSE,

    PRIMARY KEY (id),

    CONSTRAINT fk_trials_pairing
        FOREIGN KEY (pairing_id)
            REFERENCES pairings (id),

    CONSTRAINT fk_trials_correct_word
        FOREIGN KEY (correct_word_id)
            REFERENCES words (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE game_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_uuid CHAR(36) NOT NULL
        UNIQUE,
    user_id BIGINT NOT NULL,
    difficulty_level INT NOT NULL DEFAULT 1,
    condition_name VARCHAR(50) NOT NULL DEFAULT 'TEXT_ONLY',
    include_practice BOOLEAN NOT NULL DEFAULT FALSE,
    practice_answered INT NOT NULL DEFAULT 0,
    shuffle_seed BIGINT NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_game_sessions_user
        FOREIGN KEY (user_id)
            REFERENCES app_users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ADR-0/ADR-3: answers re-key to word grain (selected/target words) and
-- reference the collapsed trial. UNIQUE(session_id, trial_id) carries the 409.
CREATE TABLE player_answers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    trial_id BIGINT NOT NULL,
    selected_word_id BIGINT NOT NULL,
    target_word_id BIGINT NOT NULL,
    is_correct BOOLEAN NOT NULL,
    response_time_ms INT,
    answered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE (session_id, trial_id),

    CONSTRAINT fk_answers_session
        FOREIGN KEY (session_id)
            REFERENCES game_sessions (id),

    CONSTRAINT fk_answers_trial
        FOREIGN KEY (trial_id)
            REFERENCES trials (id),

    CONSTRAINT fk_answers_selected_word
        FOREIGN KEY (selected_word_id)
            REFERENCES words (id),

    CONSTRAINT fk_answers_target_word
        FOREIGN KEY (target_word_id)
            REFERENCES words (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Standalone 1-7 iconicity rating per word per user (thesis Rating Task).
-- ADR-0: word-keyed UNIQUE(user_id, word_id) -- the grain the instrument
-- means -- so one rating per word holds across conditions at the DB layer;
-- the nullable session_id keeps provenance without coupling to a session.
CREATE TABLE ratings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    word_id BIGINT NOT NULL,
    session_id BIGINT NULL,
    rating SMALLINT NOT NULL,
    response_time_ms INT,
    rated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE (user_id, word_id),

    CONSTRAINT fk_ratings_user
        FOREIGN KEY (user_id)
            REFERENCES app_users (id),

    CONSTRAINT fk_ratings_word
        FOREIGN KEY (word_id)
            REFERENCES words (id),

    CONSTRAINT fk_ratings_session
        FOREIGN KEY (session_id)
            REFERENCES game_sessions (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Reference data + trial content generated from src/main/resources/condition-*-choosing-sokuon.csv.
INSERT INTO languages (id, iso_code, name, family, player_note)
VALUES
(1, 'jpn', 'Japanese', 'Japonic', NULL);

INSERT INTO words (id, language_id, romaji, kana, canonical_form, canonical_script, gloss, modality, semantic_category, stimulus_file)
VALUES
(1, 1, 'gosogoso', 'ごそごそ', 'ごそごそ', 'H', 'with a rustling sound', 'AUDITORY', NULL, 'audio/a0h-gosogoso.m4a'),
(2, 1, 'katakata', 'かたかた', 'カタカタ', 'K', 'clattering, rattling', 'AUDITORY', NULL, 'audio/a0k-katakata.m4a'),
(3, 1, 'sitosito', 'しとしと', 'しとしと', 'H', 'drizzling', 'AUDITORY', NULL, 'audio/a1h-sitosito.m4a'),
(4, 1, 'batyabatya', 'ばちゃばちゃ', 'バチャバチャ', 'K', 'splashing', 'AUDITORY', NULL, 'audio/a1k-batyabatya.m4a'),
(5, 1, 'zyaazyaa', 'じゃあじゃあ', 'じゃーじゃー', 'H', 'noisily gushing', 'AUDITORY', NULL, 'audio/a2h-zyaazyaa.m4a'),
(6, 1, 'potapota', 'ぽたぽた', 'ポタポタ', 'K', 'dripping, trickling', 'AUDITORY', NULL, 'audio/a2k-potapota.m4a'),
(7, 1, 'ziriziri', 'じりじり', 'じりじり', 'H', 'sizzling, oozing', 'AUDITORY', NULL, 'audio/a3h-ziriziri.m4a'),
(8, 1, 'syakisyaki', 'しゃきしゃき', 'シャキシャキ', 'K', 'crisp, crunchy', 'AUDITORY', NULL, 'audio/a3k-syakisyaki.m4a'),
(9, 1, 'zuruzuru', 'ずるずる', 'ずるずる', 'H', 'with a slurp', 'AUDITORY', NULL, 'audio/a4h-zuruzuru.m4a'),
(10, 1, 'poripori', 'ぽりぽり', 'ポリポリ', 'K', 'munching, crunching', 'AUDITORY', NULL, 'audio/a4k-poripori.m4a'),
(11, 1, 'sororisorori', 'そろりそろり', 'そろりそろり', 'H', 'slowly and quietly', 'AUDITORY', NULL, 'audio/a5h-sororisorori.m4a'),
(12, 1, 'dotabata', 'どたばた', 'ドタバタ', 'K', 'noisily, with heavy feet', 'AUDITORY', NULL, 'audio/a5k-dotabata.m4a'),
(13, 1, 'dosari', 'どさり', 'どさり', 'H', 'with a thud', 'AUDITORY', NULL, 'audio/a6h-dosari.m4a'),
(14, 1, 'katiQ', 'かちっ', 'カチッ', 'K', 'click, snap', 'AUDITORY', NULL, 'audio/a6k-katiQ.m4a'),
(15, 1, 'dosidosi', 'どしどし', 'どしどし', 'H', 'tramping, stomping', 'AUDITORY', NULL, 'audio/a7h-dosidosi.m4a'),
(16, 1, 'gisigisi', 'ぎしぎし', 'ギシギシ', 'K', 'with a creak, squeak', 'AUDITORY', NULL, 'audio/a7k-gisigisi.m4a'),
(17, 1, 'bosori', 'ぼそり', 'ぼそり', 'H', 'in a whisper, in a murmur', 'AUDITORY', NULL, 'audio/a8h-bosori.m4a'),
(18, 1, 'kyaakyaa', 'きゃあきゃあ', 'キャーキャー', 'K', 'shrieking, squealing', 'AUDITORY', NULL, 'audio/a8k-kyaakyaa.m4a'),
(19, 1, 'mogomogo', 'もごもご', 'もごもご', 'H', 'mumbling, chewing', 'AUDITORY', NULL, 'audio/a9h-mogomogo.m4a'),
(20, 1, 'sakuQ', 'さくっ', 'サクッ', 'K', 'with a crunch', 'AUDITORY', NULL, 'audio/a9k-sakuQ.m4a'),
(21, 1, 'kukkiri', 'くっきり', 'くっきり', 'H', 'clearly, distinctly, sharply', 'VISUAL', NULL, 'audio/v0h-kukkiri.m4a'),
(22, 1, 'tiratira', 'ちらちら', 'チラチラ', 'K', 'flickering, fluttering', 'VISUAL', NULL, 'audio/v0k-tiratira.m4a'),
(23, 1, 'kurikuri', 'くりくり', 'くりくり', 'H', 'big and round', 'VISUAL', NULL, 'audio/v1h-kurikuri.m4a'),
(24, 1, 'gizagiza', 'ぎざぎざ', 'ギザギザ', 'K', 'jagged, serrated', 'VISUAL', NULL, 'audio/v1k-gizagiza.m4a'),
(25, 1, 'doNyori', 'どんより', 'どんより', 'H', 'dark, gloomy', 'VISUAL', NULL, 'audio/v2h-doNyori.m4a'),
(26, 1, 'kirakira', 'きらきら', 'キラキラ', 'K', 'glittering, sparkling', 'VISUAL', NULL, 'audio/v2k-kirakira.m4a'),
(27, 1, 'pitari', 'ぴたり', 'ぴたり', 'H', 'tightly connected, no space in between', 'VISUAL', NULL, 'audio/v3h-pitari.m4a'),
(28, 1, 'hirahira', 'ひらひら', 'ヒラヒラ', 'K', 'fluttering, dangling', 'VISUAL', NULL, 'audio/v3k-hirahira.m4a'),
(29, 1, 'bissiri', 'びっしり', 'びっしり', 'H', 'closely lined up, densely', 'VISUAL', NULL, 'audio/v4h-bissiri.m4a'),
(30, 1, 'barabara', 'ばらばら', 'バラバラ', 'K', 'scattered, in pieces', 'VISUAL', NULL, 'audio/v4k-barabara.m4a'),
(31, 1, 'busuri', 'ぶすり', 'ぶすり', 'H', 'poutingly, displeasure', 'VISUAL', NULL, 'audio/v5h-busuri.m4a'),
(32, 1, 'nikoniko', 'にこにこ', 'ニコニコ', 'K', 'smilingly, with a grin', 'VISUAL', NULL, 'audio/v5k-nikoniko.m4a'),
(33, 1, 'huNwari', 'ふんわり', 'ふんわり', 'H', 'gently, airily, fluffy', 'VISUAL', NULL, 'audio/v6h-huNwari.m4a'),
(34, 1, 'gotugotu', 'ごつごつ', 'ゴツゴツ', 'K', 'rugged, scragged, angular', 'VISUAL', NULL, 'audio/v6k-gotugotu.m4a'),
(35, 1, 'boNyari', 'ぼんやり', 'ぼんやり', 'H', 'dim, faint, indistinct', 'VISUAL', NULL, 'audio/v7h-boNyari.m4a'),
(36, 1, 'kiriQ', 'きりっ', 'キリッ', 'K', 'crisp appearance, stiffly', 'VISUAL', NULL, 'audio/v7k-kiriQ.m4a'),
(37, 1, 'marumaru', 'まるまる', 'まるまる', 'H', 'plump, rotund, chubby', 'VISUAL', NULL, 'audio/v8h-marumaru.m4a'),
(38, 1, 'garigari', 'がりがり', 'ガリガリ', 'K', 'very skinny, emaciated', 'VISUAL', NULL, 'audio/v8k-garigari.m4a'),
(39, 1, 'mukumuku', 'むくむく', 'むくむく', 'H', 'billowing, plump', 'VISUAL', NULL, 'audio/v9h-mukumuku.m4a'),
(40, 1, 'bosabosa', 'ぼさぼさ', 'ボサボサ', 'K', 'ruffled, disheveled', 'VISUAL', NULL, 'audio/v9k-bosabosa.m4a'),
(41, 1, 'uNzari', 'うんざり', 'うんざり', 'H', 'boredom, tedious, fed up with', 'INTEROCEPTIVE', NULL, 'audio/i0h-uNzari.m4a'),
(42, 1, 'wakuwaku', 'わくわく', 'ワクワク', 'K', 'excitement, nervous', 'INTEROCEPTIVE', NULL, 'audio/i0k-wakuwaku.m4a'),
(43, 1, 'geNnari', 'げんなり', 'げんなり', 'H', 'weary, fed up, dejected', 'INTEROCEPTIVE', NULL, 'audio/i1h-geNnari.m4a'),
(44, 1, 'gatugatu', 'がつがつ', 'ガツガツ', 'K', 'ravenously, greedily', 'INTEROCEPTIVE', NULL, 'audio/i1k-gatugatu.m4a'),
(45, 1, 'syoboN', 'しょぼん', 'しょぼん', 'H', 'downhearted, dejected', 'INTEROCEPTIVE', NULL, 'audio/i2h-syoboN.m4a'),
(46, 1, 'ruNruN', 'るんるん', 'ルンルン', 'K', 'happy, elated, euphoric', 'INTEROCEPTIVE', NULL, 'audio/i2k-ruNruN.m4a'),
(47, 1, 'sukkiri', 'すっきり', 'すっきり', 'H', 'refreshingly, with a feeling of relief', 'INTEROCEPTIVE', NULL, 'audio/i3h-sukkiri.m4a'),
(48, 1, 'iraira', 'いらいら', 'イライラ', 'K', 'to get irritated, annoyed', 'INTEROCEPTIVE', NULL, 'audio/i3k-iraira.m4a'),
(49, 1, 'tyaNto', 'ちゃんと', 'ちゃんと', 'H', 'attentive, proper attitude', 'INTEROCEPTIVE', NULL, 'audio/i4h-tyaNto.m4a'),
(50, 1, 'dogimagi', 'どぎまぎ', 'ドギマギ', 'K', 'flurried, nervous', 'INTEROCEPTIVE', NULL, 'audio/i4k-dogimagi.m4a'),
(51, 1, 'nobinobi', 'のびのび', 'のびのび', 'H', 'comfortable, peaceful', 'INTEROCEPTIVE', NULL, 'audio/i5h-nobinobi.m4a'),
(52, 1, 'harahara', 'はらはら', 'ハラハラ', 'K', 'anxious, nervous', 'INTEROCEPTIVE', NULL, 'audio/i5k-harahara.m4a'),
(53, 1, 'noNbiri', 'のんびり', 'のんびり', 'H', 'at leisure, in a relaxed manner', 'INTEROCEPTIVE', NULL, 'audio/i6h-noNbiri.m4a'),
(54, 1, 'girigiri', 'ぎりぎり', 'ギリギリ', 'K', 'just barely, at the last moment', 'INTEROCEPTIVE', NULL, 'audio/i6k-girigiri.m4a'),
(55, 1, 'hokkori', 'ほっこり', 'ほっこり', 'H', 'feeling warm and fluffy, soft', 'INTEROCEPTIVE', NULL, 'audio/i7h-hokkori.m4a'),
(56, 1, 'zokuzoku', 'ぞくぞく', 'ゾクゾク', 'K', 'shivering, feeling chilly', 'INTEROCEPTIVE', NULL, 'audio/i7k-zokuzoku.m4a'),
(57, 1, 'hoQ', 'ほっ', 'ほっ', 'H', 'with a feeling of relief', 'INTEROCEPTIVE', NULL, 'audio/i8h-hoQ.m4a'),
(58, 1, 'dokiri', 'どきり', 'ドキリ', 'K', 'being startled, getting a shock', 'INTEROCEPTIVE', NULL, 'audio/i8k-dokiri.m4a'),
(59, 1, 'yuttari', 'ゆったり', 'ゆったり', 'H', 'comfortable, calm, relaxed', 'INTEROCEPTIVE', NULL, 'audio/i9h-yuttari.m4a'),
(60, 1, 'dokidoki', 'どきどき', 'ドキドキ', 'K', 'with a rapid heartbeat', 'INTEROCEPTIVE', NULL, 'audio/i9k-dokidoki.m4a'),
(61, 1, 'sotto', 'そっと', 'そっと', 'H', 'softly, gently', 'AUDITORY', NULL, 'audio/p0h-sotto.m4a'),
(62, 1, 'gataN', 'がたん', 'ガタン', 'K', 'with a bang', 'AUDITORY', NULL, 'audio/p0k-gataN.m4a'),
(63, 1, 'zitto', 'じっと', 'じっと', 'H', 'motionless, fixedly', 'VISUAL', NULL, 'audio/p1h-zitto.m4a'),
(64, 1, 'paQ', 'ぱっ', 'パッ', 'K', 'suddenly, in a flash', 'VISUAL', NULL, 'audio/p1k-paQ.m4a'),
(65, 1, 'sorosoro', 'そろそろ', 'そろそろ', 'H', 'slowly, quietly', 'AUDITORY', NULL, 'audio/p2h-sorosoro.m4a'),
(66, 1, 'gaNgaN', 'がんがん', 'ガンガン', 'K', 'clanging, banging', 'AUDITORY', NULL, 'audio/p2k-gaNgaN.m4a'),
(67, 1, 'sokkuri', 'そっくり', 'そっくり', 'H', 'exactly alike, spitting image', 'VISUAL', NULL, 'audio/p3h-sokkuri.m4a'),
(68, 1, 'garari', 'がらり', 'ガラリ', 'K', 'completely, totally changed', 'VISUAL', NULL, 'audio/p3k-garari.m4a');

INSERT INTO presentations (id, word_id, condition_name, display_form, script_code)
VALUES
(1, 1, 'CONDITION_1_SOKUON', 'ごそごそ', 'HU'),
(2, 2, 'CONDITION_1_SOKUON', 'カタカタ', 'KD'),
(3, 3, 'CONDITION_1_SOKUON', 'しとしと', 'HU'),
(4, 4, 'CONDITION_1_SOKUON', 'バチャバチャ', 'KD'),
(5, 5, 'CONDITION_1_SOKUON', 'じゃーじゃー', 'HU'),
(6, 6, 'CONDITION_1_SOKUON', 'ポタポタ', 'KD'),
(7, 7, 'CONDITION_1_SOKUON', 'じりじり', 'HU'),
(8, 8, 'CONDITION_1_SOKUON', 'シャキシャキ', 'KD'),
(9, 9, 'CONDITION_1_SOKUON', 'ずるずる', 'HU'),
(10, 10, 'CONDITION_1_SOKUON', 'ポリポリ', 'KD'),
(11, 11, 'CONDITION_1_SOKUON', 'そろりそろり', 'HU'),
(12, 12, 'CONDITION_1_SOKUON', 'ドタバタ', 'KD'),
(13, 13, 'CONDITION_1_SOKUON', 'どさり', 'HU'),
(14, 14, 'CONDITION_1_SOKUON', 'カチッ', 'KD'),
(15, 15, 'CONDITION_1_SOKUON', 'どしどし', 'HU'),
(16, 16, 'CONDITION_1_SOKUON', 'ギシギシ', 'KD'),
(17, 17, 'CONDITION_1_SOKUON', 'ぼそり', 'HU'),
(18, 18, 'CONDITION_1_SOKUON', 'キャーキャー', 'KD'),
(19, 19, 'CONDITION_1_SOKUON', 'もごもご', 'HU'),
(20, 20, 'CONDITION_1_SOKUON', 'サクッ', 'KD'),
(21, 21, 'CONDITION_1_SOKUON', 'くっきり', 'HU'),
(22, 22, 'CONDITION_1_SOKUON', 'チラチラ', 'KD'),
(23, 23, 'CONDITION_1_SOKUON', 'くりくり', 'HU'),
(24, 24, 'CONDITION_1_SOKUON', 'ギザギザ', 'KD'),
(25, 25, 'CONDITION_1_SOKUON', 'どんより', 'HU'),
(26, 26, 'CONDITION_1_SOKUON', 'キラキラ', 'KD'),
(27, 27, 'CONDITION_1_SOKUON', 'ぴたり', 'HU'),
(28, 28, 'CONDITION_1_SOKUON', 'ヒラヒラ', 'KD'),
(29, 29, 'CONDITION_1_SOKUON', 'びっしり', 'HU'),
(30, 30, 'CONDITION_1_SOKUON', 'バラバラ', 'KD'),
(31, 31, 'CONDITION_1_SOKUON', 'ぶすり', 'HU'),
(32, 32, 'CONDITION_1_SOKUON', 'ニコニコ', 'KD'),
(33, 33, 'CONDITION_1_SOKUON', 'ふんわり', 'HU'),
(34, 34, 'CONDITION_1_SOKUON', 'ゴツゴツ', 'KD'),
(35, 35, 'CONDITION_1_SOKUON', 'ぼんやり', 'HU'),
(36, 36, 'CONDITION_1_SOKUON', 'キリッ', 'KD'),
(37, 37, 'CONDITION_1_SOKUON', 'まるまる', 'HU'),
(38, 38, 'CONDITION_1_SOKUON', 'ガリガリ', 'KD'),
(39, 39, 'CONDITION_1_SOKUON', 'むくむく', 'HU'),
(40, 40, 'CONDITION_1_SOKUON', 'ボサボサ', 'KD'),
(41, 41, 'CONDITION_1_SOKUON', 'うんざり', 'HU'),
(42, 42, 'CONDITION_1_SOKUON', 'ワクワク', 'KD'),
(43, 43, 'CONDITION_1_SOKUON', 'げんなり', 'HU'),
(44, 44, 'CONDITION_1_SOKUON', 'ガツガツ', 'KD'),
(45, 45, 'CONDITION_1_SOKUON', 'しょぼん', 'HU'),
(46, 46, 'CONDITION_1_SOKUON', 'ルンルン', 'KD'),
(47, 47, 'CONDITION_1_SOKUON', 'すっきり', 'HU'),
(48, 48, 'CONDITION_1_SOKUON', 'イライラ', 'KD'),
(49, 49, 'CONDITION_1_SOKUON', 'ちゃんと', 'HU'),
(50, 50, 'CONDITION_1_SOKUON', 'ドギマギ', 'KD'),
(51, 51, 'CONDITION_1_SOKUON', 'のびのび', 'HU'),
(52, 52, 'CONDITION_1_SOKUON', 'ハラハラ', 'KD'),
(53, 53, 'CONDITION_1_SOKUON', 'のんびり', 'HU'),
(54, 54, 'CONDITION_1_SOKUON', 'ギリギリ', 'KD'),
(55, 55, 'CONDITION_1_SOKUON', 'ほっこり', 'HU'),
(56, 56, 'CONDITION_1_SOKUON', 'ゾクゾク', 'KD'),
(57, 57, 'CONDITION_1_SOKUON', 'ほっ', 'HU'),
(58, 58, 'CONDITION_1_SOKUON', 'ドキリ', 'KD'),
(59, 59, 'CONDITION_1_SOKUON', 'ゆったり', 'HU'),
(60, 60, 'CONDITION_1_SOKUON', 'ドキドキ', 'KD'),
(61, 1, 'CONDITION_2_SOKUON', 'ごそごそ', 'HH'),
(62, 2, 'CONDITION_2_SOKUON', 'カタカタ', 'KK'),
(63, 3, 'CONDITION_2_SOKUON', 'しとしと', 'HH'),
(64, 4, 'CONDITION_2_SOKUON', 'バチャバチャ', 'KK'),
(65, 5, 'CONDITION_2_SOKUON', 'じゃーじゃー', 'HH'),
(66, 6, 'CONDITION_2_SOKUON', 'ポタポタ', 'KK'),
(67, 7, 'CONDITION_2_SOKUON', 'じりじり', 'HH'),
(68, 8, 'CONDITION_2_SOKUON', 'シャキシャキ', 'KK'),
(69, 9, 'CONDITION_2_SOKUON', 'ずるずる', 'HH'),
(70, 10, 'CONDITION_2_SOKUON', 'ポリポリ', 'KK'),
(71, 11, 'CONDITION_2_SOKUON', 'そろりそろり', 'HH'),
(72, 12, 'CONDITION_2_SOKUON', 'ドタバタ', 'KK'),
(73, 13, 'CONDITION_2_SOKUON', 'どさり', 'HH'),
(74, 14, 'CONDITION_2_SOKUON', 'カチッ', 'KK'),
(75, 15, 'CONDITION_2_SOKUON', 'どしどし', 'HH'),
(76, 16, 'CONDITION_2_SOKUON', 'ギシギシ', 'KK'),
(77, 17, 'CONDITION_2_SOKUON', 'ぼそり', 'HH'),
(78, 18, 'CONDITION_2_SOKUON', 'キャーキャー', 'KK'),
(79, 19, 'CONDITION_2_SOKUON', 'もごもご', 'HH'),
(80, 20, 'CONDITION_2_SOKUON', 'サクッ', 'KK'),
(81, 21, 'CONDITION_2_SOKUON', 'くっきり', 'HH'),
(82, 22, 'CONDITION_2_SOKUON', 'チラチラ', 'KK'),
(83, 23, 'CONDITION_2_SOKUON', 'くりくり', 'HH'),
(84, 24, 'CONDITION_2_SOKUON', 'ギザギザ', 'KK'),
(85, 25, 'CONDITION_2_SOKUON', 'どんより', 'HH'),
(86, 26, 'CONDITION_2_SOKUON', 'キラキラ', 'KK'),
(87, 27, 'CONDITION_2_SOKUON', 'ぴたり', 'HH'),
(88, 28, 'CONDITION_2_SOKUON', 'ヒラヒラ', 'KK'),
(89, 29, 'CONDITION_2_SOKUON', 'びっしり', 'HH'),
(90, 30, 'CONDITION_2_SOKUON', 'バラバラ', 'KK'),
(91, 31, 'CONDITION_2_SOKUON', 'ぶすり', 'HH'),
(92, 32, 'CONDITION_2_SOKUON', 'ニコニコ', 'KK'),
(93, 33, 'CONDITION_2_SOKUON', 'ふんわり', 'HH'),
(94, 34, 'CONDITION_2_SOKUON', 'ゴツゴツ', 'KK'),
(95, 35, 'CONDITION_2_SOKUON', 'ぼんやり', 'HH'),
(96, 36, 'CONDITION_2_SOKUON', 'キリッ', 'KK'),
(97, 37, 'CONDITION_2_SOKUON', 'まるまる', 'HH'),
(98, 38, 'CONDITION_2_SOKUON', 'ガリガリ', 'KK'),
(99, 39, 'CONDITION_2_SOKUON', 'むくむく', 'HH'),
(100, 40, 'CONDITION_2_SOKUON', 'ボサボサ', 'KK'),
(101, 41, 'CONDITION_2_SOKUON', 'うんざり', 'HH'),
(102, 42, 'CONDITION_2_SOKUON', 'ワクワク', 'KK'),
(103, 43, 'CONDITION_2_SOKUON', 'げんなり', 'HH'),
(104, 44, 'CONDITION_2_SOKUON', 'ガツガツ', 'KK'),
(105, 45, 'CONDITION_2_SOKUON', 'しょぼん', 'HH'),
(106, 46, 'CONDITION_2_SOKUON', 'ルンルン', 'KK'),
(107, 47, 'CONDITION_2_SOKUON', 'すっきり', 'HH'),
(108, 48, 'CONDITION_2_SOKUON', 'イライラ', 'KK'),
(109, 49, 'CONDITION_2_SOKUON', 'ちゃんと', 'HH'),
(110, 50, 'CONDITION_2_SOKUON', 'ドギマギ', 'KK'),
(111, 51, 'CONDITION_2_SOKUON', 'のびのび', 'HH'),
(112, 52, 'CONDITION_2_SOKUON', 'ハラハラ', 'KK'),
(113, 53, 'CONDITION_2_SOKUON', 'のんびり', 'HH'),
(114, 54, 'CONDITION_2_SOKUON', 'ギリギリ', 'KK'),
(115, 55, 'CONDITION_2_SOKUON', 'ほっこり', 'HH'),
(116, 56, 'CONDITION_2_SOKUON', 'ゾクゾク', 'KK'),
(117, 57, 'CONDITION_2_SOKUON', 'ほっ', 'HH'),
(118, 58, 'CONDITION_2_SOKUON', 'ドキリ', 'KK'),
(119, 59, 'CONDITION_2_SOKUON', 'ゆったり', 'HH'),
(120, 60, 'CONDITION_2_SOKUON', 'ドキドキ', 'KK'),
(121, 1, 'CONDITION_3_SOKUON', 'ゴソゴソ', 'HK'),
(122, 2, 'CONDITION_3_SOKUON', 'かたかた', 'KH'),
(123, 3, 'CONDITION_3_SOKUON', 'シトシト', 'HK'),
(124, 4, 'CONDITION_3_SOKUON', 'ばちゃばちゃ', 'KH'),
(125, 5, 'CONDITION_3_SOKUON', 'ジャージャー', 'HK'),
(126, 6, 'CONDITION_3_SOKUON', 'ぽたぽた', 'KH'),
(127, 7, 'CONDITION_3_SOKUON', 'ジリジリ', 'HK'),
(128, 8, 'CONDITION_3_SOKUON', 'しゃきしゃき', 'KH'),
(129, 9, 'CONDITION_3_SOKUON', 'ズルズル', 'HK'),
(130, 10, 'CONDITION_3_SOKUON', 'ぽりぽり', 'KH'),
(131, 11, 'CONDITION_3_SOKUON', 'ソロリソロリ', 'HK'),
(132, 12, 'CONDITION_3_SOKUON', 'どたばた', 'KH'),
(133, 13, 'CONDITION_3_SOKUON', 'ドサリ', 'HK'),
(134, 14, 'CONDITION_3_SOKUON', 'かちっ', 'KH'),
(135, 15, 'CONDITION_3_SOKUON', 'ドシドシ', 'HK'),
(136, 16, 'CONDITION_3_SOKUON', 'ぎしぎし', 'KH'),
(137, 17, 'CONDITION_3_SOKUON', 'ボソリ', 'HK'),
(138, 18, 'CONDITION_3_SOKUON', 'きゃーきゃー', 'KH'),
(139, 19, 'CONDITION_3_SOKUON', 'モゴモゴ', 'HK'),
(140, 20, 'CONDITION_3_SOKUON', 'さくっ', 'KH'),
(141, 21, 'CONDITION_3_SOKUON', 'クッキリ', 'HK'),
(142, 22, 'CONDITION_3_SOKUON', 'ちらちら', 'KH'),
(143, 23, 'CONDITION_3_SOKUON', 'クリクリ', 'HK'),
(144, 24, 'CONDITION_3_SOKUON', 'ぎざぎざ', 'KH'),
(145, 25, 'CONDITION_3_SOKUON', 'ドンヨリ', 'HK'),
(146, 26, 'CONDITION_3_SOKUON', 'きらきら', 'KH'),
(147, 27, 'CONDITION_3_SOKUON', 'ピタリ', 'HK'),
(148, 28, 'CONDITION_3_SOKUON', 'ひらひら', 'KH'),
(149, 29, 'CONDITION_3_SOKUON', 'ビッシリ', 'HK'),
(150, 30, 'CONDITION_3_SOKUON', 'ばらばら', 'KH'),
(151, 31, 'CONDITION_3_SOKUON', 'ブスリ', 'HK'),
(152, 32, 'CONDITION_3_SOKUON', 'にこにこ', 'KH'),
(153, 33, 'CONDITION_3_SOKUON', 'フンワリ', 'HK'),
(154, 34, 'CONDITION_3_SOKUON', 'ごつごつ', 'KH'),
(155, 35, 'CONDITION_3_SOKUON', 'ボンヤリ', 'HK'),
(156, 36, 'CONDITION_3_SOKUON', 'きりっ', 'KH'),
(157, 37, 'CONDITION_3_SOKUON', 'マルマル', 'HK'),
(158, 38, 'CONDITION_3_SOKUON', 'がりがり', 'KH'),
(159, 39, 'CONDITION_3_SOKUON', 'ムクムク', 'HK'),
(160, 40, 'CONDITION_3_SOKUON', 'ぼさぼさ', 'KH'),
(161, 41, 'CONDITION_3_SOKUON', 'ウンザリ', 'HK'),
(162, 42, 'CONDITION_3_SOKUON', 'わくわく', 'KH'),
(163, 43, 'CONDITION_3_SOKUON', 'ゲンナリ', 'HK'),
(164, 44, 'CONDITION_3_SOKUON', 'がつがつ', 'KH'),
(165, 45, 'CONDITION_3_SOKUON', 'ショボン', 'HK'),
(166, 46, 'CONDITION_3_SOKUON', 'るんるん', 'KH'),
(167, 47, 'CONDITION_3_SOKUON', 'スッキリ', 'HK'),
(168, 48, 'CONDITION_3_SOKUON', 'いらいら', 'KH'),
(169, 49, 'CONDITION_3_SOKUON', 'チャント', 'HK'),
(170, 50, 'CONDITION_3_SOKUON', 'どぎまぎ', 'KH'),
(171, 51, 'CONDITION_3_SOKUON', 'ノビノビ', 'HK'),
(172, 52, 'CONDITION_3_SOKUON', 'はらはら', 'KH'),
(173, 53, 'CONDITION_3_SOKUON', 'ノンビリ', 'HK'),
(174, 54, 'CONDITION_3_SOKUON', 'ぎりぎり', 'KH'),
(175, 55, 'CONDITION_3_SOKUON', 'ホッコリ', 'HK'),
(176, 56, 'CONDITION_3_SOKUON', 'ぞくぞく', 'KH'),
(177, 57, 'CONDITION_3_SOKUON', 'ホッ', 'HK'),
(178, 58, 'CONDITION_3_SOKUON', 'どきり', 'KH'),
(179, 59, 'CONDITION_3_SOKUON', 'ユッタリ', 'HK'),
(180, 60, 'CONDITION_3_SOKUON', 'どきどき', 'KH'),
(181, 61, 'CONDITION_1_SOKUON', 'そっと', 'HU'),
(182, 62, 'CONDITION_1_SOKUON', 'ガタン', 'KD'),
(183, 63, 'CONDITION_1_SOKUON', 'じっと', 'HU'),
(184, 64, 'CONDITION_1_SOKUON', 'パッ', 'KD'),
(185, 65, 'CONDITION_1_SOKUON', 'そろそろ', 'HU'),
(186, 66, 'CONDITION_1_SOKUON', 'ガンガン', 'KD'),
(187, 67, 'CONDITION_1_SOKUON', 'そっくり', 'HU'),
(188, 68, 'CONDITION_1_SOKUON', 'ガラリ', 'KD'),
(189, 61, 'CONDITION_2_SOKUON', 'そっと', 'HH'),
(190, 62, 'CONDITION_2_SOKUON', 'ガタン', 'KK'),
(191, 63, 'CONDITION_2_SOKUON', 'じっと', 'HH'),
(192, 64, 'CONDITION_2_SOKUON', 'パッ', 'KK'),
(193, 65, 'CONDITION_2_SOKUON', 'そろそろ', 'HH'),
(194, 66, 'CONDITION_2_SOKUON', 'ガンガン', 'KK'),
(195, 67, 'CONDITION_2_SOKUON', 'そっくり', 'HH'),
(196, 68, 'CONDITION_2_SOKUON', 'ガラリ', 'KK'),
(197, 61, 'CONDITION_3_SOKUON', 'ソット', 'HK'),
(198, 62, 'CONDITION_3_SOKUON', 'がたん', 'KH'),
(199, 63, 'CONDITION_3_SOKUON', 'ジット', 'HK'),
(200, 64, 'CONDITION_3_SOKUON', 'ぱっ', 'KH'),
(201, 65, 'CONDITION_3_SOKUON', 'ソロソロ', 'HK'),
(202, 66, 'CONDITION_3_SOKUON', 'がんがん', 'KH'),
(203, 67, 'CONDITION_3_SOKUON', 'ソックリ', 'HK'),
(204, 68, 'CONDITION_3_SOKUON', 'がらり', 'KH');

INSERT INTO pairings (id, pair_code, language_id, word_a_id, word_b_id, modality, is_core, source, difficulty_prior, thesis_accuracy, foil_distance, signoff_ref, approved_at)
VALUES
(1, 'a0', 1, 1, 2, 'AUDITORY', 1, 'THESIS', NULL, 0.6944, NULL, NULL, NULL),
(2, 'a1', 1, 3, 4, 'AUDITORY', 1, 'THESIS', NULL, 0.5833, NULL, NULL, NULL),
(3, 'a2', 1, 5, 6, 'AUDITORY', 1, 'THESIS', NULL, 0.7222, NULL, NULL, NULL),
(4, 'a3', 1, 7, 8, 'AUDITORY', 1, 'THESIS', NULL, 0.6667, NULL, NULL, NULL),
(5, 'a4', 1, 9, 10, 'AUDITORY', 1, 'THESIS', NULL, 0.6389, NULL, NULL, NULL),
(6, 'a5', 1, 11, 12, 'AUDITORY', 1, 'THESIS', NULL, 0.5000, NULL, NULL, NULL),
(7, 'a6', 1, 13, 14, 'AUDITORY', 1, 'THESIS', NULL, 0.6667, NULL, NULL, NULL),
(8, 'a7', 1, 15, 16, 'AUDITORY', 1, 'THESIS', NULL, 0.8333, NULL, NULL, NULL),
(9, 'a8', 1, 17, 18, 'AUDITORY', 1, 'THESIS', NULL, 0.6111, NULL, NULL, NULL),
(10, 'a9', 1, 19, 20, 'AUDITORY', 1, 'THESIS', NULL, 0.9444, NULL, NULL, NULL),
(11, 'v0', 1, 21, 22, 'VISUAL', 1, 'THESIS', NULL, 0.7222, NULL, NULL, NULL),
(12, 'v1', 1, 23, 24, 'VISUAL', 1, 'THESIS', NULL, 0.5833, NULL, NULL, NULL),
(13, 'v2', 1, 25, 26, 'VISUAL', 1, 'THESIS', NULL, 0.8333, NULL, NULL, NULL),
(14, 'v3', 1, 27, 28, 'VISUAL', 1, 'THESIS', NULL, 0.5833, NULL, NULL, NULL),
(15, 'v4', 1, 29, 30, 'VISUAL', 1, 'THESIS', NULL, 0.4722, NULL, NULL, NULL),
(16, 'v5', 1, 31, 32, 'VISUAL', 1, 'THESIS', NULL, 0.6667, NULL, NULL, NULL),
(17, 'v6', 1, 33, 34, 'VISUAL', 1, 'THESIS', NULL, 0.5556, NULL, NULL, NULL),
(18, 'v7', 1, 35, 36, 'VISUAL', 1, 'THESIS', NULL, 0.6944, NULL, NULL, NULL),
(19, 'v8', 1, 37, 38, 'VISUAL', 1, 'THESIS', NULL, 0.7500, NULL, NULL, NULL),
(20, 'v9', 1, 39, 40, 'VISUAL', 1, 'THESIS', NULL, 0.5556, NULL, NULL, NULL),
(21, 'i0', 1, 41, 42, 'INTEROCEPTIVE', 1, 'THESIS', NULL, 0.6389, NULL, NULL, NULL),
(22, 'i1', 1, 43, 44, 'INTEROCEPTIVE', 1, 'THESIS', NULL, 0.5556, NULL, NULL, NULL),
(23, 'i2', 1, 45, 46, 'INTEROCEPTIVE', 1, 'THESIS', NULL, 0.3611, NULL, NULL, NULL),
(24, 'i3', 1, 47, 48, 'INTEROCEPTIVE', 1, 'THESIS', NULL, 0.8056, NULL, NULL, NULL),
(25, 'i4', 1, 49, 50, 'INTEROCEPTIVE', 1, 'THESIS', NULL, 0.5833, NULL, NULL, NULL),
(26, 'i5', 1, 51, 52, 'INTEROCEPTIVE', 1, 'THESIS', NULL, 0.6944, NULL, NULL, NULL),
(27, 'i6', 1, 53, 54, 'INTEROCEPTIVE', 1, 'THESIS', NULL, 0.5556, NULL, NULL, NULL),
(28, 'i7', 1, 55, 56, 'INTEROCEPTIVE', 1, 'THESIS', NULL, 0.5556, NULL, NULL, NULL),
(29, 'i8', 1, 57, 58, 'INTEROCEPTIVE', 1, 'THESIS', NULL, 0.5556, NULL, NULL, NULL),
(30, 'i9', 1, 59, 60, 'INTEROCEPTIVE', 1, 'THESIS', NULL, 0.6667, NULL, NULL, NULL),
(31, 'p0', 1, 61, 62, 'AUDITORY', 1, 'THESIS', NULL, NULL, NULL, NULL, NULL),
(32, 'p1', 1, 63, 64, 'VISUAL', 1, 'THESIS', NULL, NULL, NULL, NULL, NULL),
(33, 'p2', 1, 65, 66, 'AUDITORY', 1, 'THESIS', NULL, NULL, NULL, NULL, NULL),
(34, 'p3', 1, 67, 68, 'VISUAL', 1, 'THESIS', NULL, NULL, NULL, NULL, NULL);

INSERT INTO trials (id, round_type, pairing_id, correct_word_id, feature_axis, is_practice)
VALUES
(1, 'CHOOSING', 1, 1, NULL, 0),
(2, 'CHOOSING', 2, 4, NULL, 0),
(3, 'CHOOSING', 3, 5, NULL, 0),
(4, 'CHOOSING', 4, 8, NULL, 0),
(5, 'CHOOSING', 5, 9, NULL, 0),
(6, 'CHOOSING', 6, 12, NULL, 0),
(7, 'CHOOSING', 7, 13, NULL, 0),
(8, 'CHOOSING', 8, 16, NULL, 0),
(9, 'CHOOSING', 9, 17, NULL, 0),
(10, 'CHOOSING', 10, 20, NULL, 0),
(11, 'CHOOSING', 11, 21, NULL, 0),
(12, 'CHOOSING', 12, 24, NULL, 0),
(13, 'CHOOSING', 13, 25, NULL, 0),
(14, 'CHOOSING', 14, 28, NULL, 0),
(15, 'CHOOSING', 15, 29, NULL, 0),
(16, 'CHOOSING', 16, 32, NULL, 0),
(17, 'CHOOSING', 17, 33, NULL, 0),
(18, 'CHOOSING', 18, 36, NULL, 0),
(19, 'CHOOSING', 19, 37, NULL, 0),
(20, 'CHOOSING', 20, 40, NULL, 0),
(21, 'CHOOSING', 21, 41, NULL, 0),
(22, 'CHOOSING', 22, 44, NULL, 0),
(23, 'CHOOSING', 23, 45, NULL, 0),
(24, 'CHOOSING', 24, 48, NULL, 0),
(25, 'CHOOSING', 25, 49, NULL, 0),
(26, 'CHOOSING', 26, 52, NULL, 0),
(27, 'CHOOSING', 27, 53, NULL, 0),
(28, 'CHOOSING', 28, 56, NULL, 0),
(29, 'CHOOSING', 29, 57, NULL, 0),
(30, 'CHOOSING', 30, 60, NULL, 0),
(31, 'CHOOSING', 31, 61, NULL, 1),
(32, 'CHOOSING', 32, 64, NULL, 1),
(33, 'CHOOSING', 33, 66, NULL, 1),
(34, 'CHOOSING', 34, 67, NULL, 1);

-- Dev-only admin account. Throwaway password; see docs/demo-runbook.md, "Creating an admin".
INSERT INTO app_users (id, username, email, password_hash, role)
VALUES
(1, 'arena_admin', 'arena_admin@example.invalid', '$2a$10$AWmwnu11Xi/MVcBlbRLB8OUYrJ7kmfjW9Qzy6tCAk38/Kw0EUGzaK', 'ROLE_ADMIN');
