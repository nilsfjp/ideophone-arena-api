CREATE DATABASE IF NOT EXISTS ideophone_arena
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE ideophone_arena;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS ratings;
DROP TABLE IF EXISTS player_answers;
DROP TABLE IF EXISTS game_sessions;
DROP TABLE IF EXISTS arena_rounds;
DROP TABLE IF EXISTS ideophones;
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

CREATE TABLE ideophones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    kana VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    display_form VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    canonical_form VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    romaji VARCHAR(100) NOT NULL,
    gloss VARCHAR(255) NOT NULL,
    canonical_script VARCHAR(20) NOT NULL,
    stimulus_file VARCHAR(100) NOT NULL,
    modality VARCHAR(50),

    PRIMARY KEY (id),
    UNIQUE (kana, canonical_script)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE arena_rounds (
    id BIGINT NOT NULL AUTO_INCREMENT,
    prompt VARCHAR(255) NOT NULL,
    left_ideophone_id BIGINT NOT NULL,
    right_ideophone_id BIGINT NOT NULL,
    correct_ideophone_id BIGINT NOT NULL,
    condition_name VARCHAR(50) NOT NULL DEFAULT 'TEXT_ONLY',
    difficulty_level INT NOT NULL DEFAULT 1,
    is_practice BOOLEAN NOT NULL DEFAULT FALSE,

    PRIMARY KEY (id),

    CONSTRAINT fk_round_left_ideophone
        FOREIGN KEY (left_ideophone_id)
            REFERENCES ideophones (id),

    CONSTRAINT fk_round_right_ideophone
        FOREIGN KEY (right_ideophone_id)
            REFERENCES ideophones (id),

    CONSTRAINT fk_round_correct_ideophone
        FOREIGN KEY (correct_ideophone_id)
            REFERENCES ideophones (id)
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

CREATE TABLE player_answers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    round_id BIGINT NOT NULL,
    selected_ideophone_id BIGINT NOT NULL,
    target_ideophone_id BIGINT NOT NULL,
    is_correct BOOLEAN NOT NULL,
    response_time_ms INT,
    answered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE (session_id, round_id),

    CONSTRAINT fk_answers_session
        FOREIGN KEY (session_id)
            REFERENCES game_sessions (id),

    CONSTRAINT fk_answers_round
        FOREIGN KEY (round_id)
            REFERENCES arena_rounds (id),

    CONSTRAINT fk_answers_selected_ideophone
        FOREIGN KEY (selected_ideophone_id)
            REFERENCES ideophones (id),

    CONSTRAINT fk_answers_target_ideophone
        FOREIGN KEY (target_ideophone_id)
            REFERENCES ideophones (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Standalone 1-7 iconicity rating per word per user (thesis Rating Task).
-- User-keyed (UNIQUE(user_id, ideophone_id)) so a user's rating of a word
-- can later be joined against their guess accuracy on the same word; the
-- nullable session_id keeps provenance without coupling to a session.
CREATE TABLE ratings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    ideophone_id BIGINT NOT NULL,
    session_id BIGINT NULL,
    rating SMALLINT NOT NULL,
    response_time_ms INT,
    rated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE (user_id, ideophone_id),

    CONSTRAINT fk_ratings_user
        FOREIGN KEY (user_id)
            REFERENCES app_users (id),

    CONSTRAINT fk_ratings_ideophone
        FOREIGN KEY (ideophone_id)
            REFERENCES ideophones (id),

    CONSTRAINT fk_ratings_session
        FOREIGN KEY (session_id)
            REFERENCES game_sessions (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Seed data generated from src/main/resources/condition-*-choosing-sokuon.csv.
INSERT INTO ideophones (id, kana, display_form, canonical_form, romaji, gloss, canonical_script, stimulus_file, modality)
VALUES
(1, 'ごそごそ', 'ごそごそ', 'ごそごそ', 'gosogoso', 'with a rustling sound', 'HU', 'audio/a0h-gosogoso.m4a', 'AUDITORY'),
(2, 'かたかた', 'カタカタ', 'カタカタ', 'katakata', 'clattering, rattling', 'KD', 'audio/a0k-katakata.m4a', 'AUDITORY'),
(3, 'しとしと', 'しとしと', 'しとしと', 'sitosito', 'drizzling', 'HU', 'audio/a1h-sitosito.m4a', 'AUDITORY'),
(4, 'ばちゃばちゃ', 'バチャバチャ', 'バチャバチャ', 'batyabatya', 'splashing', 'KD', 'audio/a1k-batyabatya.m4a', 'AUDITORY'),
(5, 'じゃあじゃあ', 'じゃーじゃー', 'じゃーじゃー', 'zyaazyaa', 'noisily gushing', 'HU', 'audio/a2h-zyaazyaa.m4a', 'AUDITORY'),
(6, 'ぽたぽた', 'ポタポタ', 'ポタポタ', 'potapota', 'dripping, trickling', 'KD', 'audio/a2k-potapota.m4a', 'AUDITORY'),
(7, 'じりじり', 'じりじり', 'じりじり', 'ziriziri', 'sizzling, oozing', 'HU', 'audio/a3h-ziriziri.m4a', 'AUDITORY'),
(8, 'しゃきしゃき', 'シャキシャキ', 'シャキシャキ', 'syakisyaki', 'crisp, crunchy', 'KD', 'audio/a3k-syakisyaki.m4a', 'AUDITORY'),
(9, 'ずるずる', 'ずるずる', 'ずるずる', 'zuruzuru', 'with a slurp', 'HU', 'audio/a4h-zuruzuru.m4a', 'AUDITORY'),
(10, 'ぽりぽり', 'ポリポリ', 'ポリポリ', 'poripori', 'munching, crunching', 'KD', 'audio/a4k-poripori.m4a', 'AUDITORY'),
(11, 'そろりそろり', 'そろりそろり', 'そろりそろり', 'sororisorori', 'slowly and quietly', 'HU', 'audio/a5h-sororisorori.m4a', 'AUDITORY'),
(12, 'どたばた', 'ドタバタ', 'ドタバタ', 'dotabata', 'noisily, with heavy feet', 'KD', 'audio/a5k-dotabata.m4a', 'AUDITORY'),
(13, 'どさり', 'どさり', 'どさり', 'dosari', 'with a thud', 'HU', 'audio/a6h-dosari.m4a', 'AUDITORY'),
(14, 'かちっ', 'カチッ', 'カチッ', 'katiQ', 'click, snap', 'KD', 'audio/a6k-katiQ.m4a', 'AUDITORY'),
(15, 'どしどし', 'どしどし', 'どしどし', 'dosidosi', 'tramping, stomping', 'HU', 'audio/a7h-dosidosi.m4a', 'AUDITORY'),
(16, 'ぎしぎし', 'ギシギシ', 'ギシギシ', 'gisigisi', 'with a creak, squeak', 'KD', 'audio/a7k-gisigisi.m4a', 'AUDITORY'),
(17, 'ぼそり', 'ぼそり', 'ぼそり', 'bosori', 'in a whisper, in a murmur', 'HU', 'audio/a8h-bosori.m4a', 'AUDITORY'),
(18, 'きゃあきゃあ', 'キャーキャー', 'キャーキャー', 'kyaakyaa', 'shrieking, squealing', 'KD', 'audio/a8k-kyaakyaa.m4a', 'AUDITORY'),
(19, 'もごもご', 'もごもご', 'もごもご', 'mogomogo', 'mumbling, chewing', 'HU', 'audio/a9h-mogomogo.m4a', 'AUDITORY'),
(20, 'さくっ', 'サクッ', 'サクッ', 'sakuQ', 'with a crunch', 'KD', 'audio/a9k-sakuQ.m4a', 'AUDITORY'),
(21, 'くっきり', 'くっきり', 'くっきり', 'kukkiri', 'clearly, distinctly, sharply', 'HU', 'audio/v0h-kukkiri.m4a', 'VISUAL'),
(22, 'ちらちら', 'チラチラ', 'チラチラ', 'tiratira', 'flickering, fluttering', 'KD', 'audio/v0k-tiratira.m4a', 'VISUAL'),
(23, 'くりくり', 'くりくり', 'くりくり', 'kurikuri', 'big and round', 'HU', 'audio/v1h-kurikuri.m4a', 'VISUAL'),
(24, 'ぎざぎざ', 'ギザギザ', 'ギザギザ', 'gizagiza', 'jagged, serrated', 'KD', 'audio/v1k-gizagiza.m4a', 'VISUAL'),
(25, 'どんより', 'どんより', 'どんより', 'doNyori', 'dark, gloomy', 'HU', 'audio/v2h-doNyori.m4a', 'VISUAL'),
(26, 'きらきら', 'キラキラ', 'キラキラ', 'kirakira', 'glittering, sparkling', 'KD', 'audio/v2k-kirakira.m4a', 'VISUAL'),
(27, 'ぴたり', 'ぴたり', 'ぴたり', 'pitari', 'tightly connected, no space in between', 'HU', 'audio/v3h-pitari.m4a', 'VISUAL'),
(28, 'ひらひら', 'ヒラヒラ', 'ヒラヒラ', 'hirahira', 'fluttering, dangling', 'KD', 'audio/v3k-hirahira.m4a', 'VISUAL'),
(29, 'びっしり', 'びっしり', 'びっしり', 'bissiri', 'closely lined up, densely', 'HU', 'audio/v4h-bissiri.m4a', 'VISUAL'),
(30, 'ばらばら', 'バラバラ', 'バラバラ', 'barabara', 'scattered, in pieces', 'KD', 'audio/v4k-barabara.m4a', 'VISUAL'),
(31, 'ぶすり', 'ぶすり', 'ぶすり', 'busuri', 'poutingly, displeasure', 'HU', 'audio/v5h-busuri.m4a', 'VISUAL'),
(32, 'にこにこ', 'ニコニコ', 'ニコニコ', 'nikoniko', 'smilingly, with a grin', 'KD', 'audio/v5k-nikoniko.m4a', 'VISUAL'),
(33, 'ふんわり', 'ふんわり', 'ふんわり', 'huNwari', 'gently, airily, fluffy', 'HU', 'audio/v6h-huNwari.m4a', 'VISUAL'),
(34, 'ごつごつ', 'ゴツゴツ', 'ゴツゴツ', 'gotugotu', 'rugged, scragged, angular', 'KD', 'audio/v6k-gotugotu.m4a', 'VISUAL'),
(35, 'ぼんやり', 'ぼんやり', 'ぼんやり', 'boNyari', 'dim, faint, indistinct', 'HU', 'audio/v7h-boNyari.m4a', 'VISUAL'),
(36, 'きりっ', 'キリッ', 'キリッ', 'kiriQ', 'crisp appearance, stiffly', 'KD', 'audio/v7k-kiriQ.m4a', 'VISUAL'),
(37, 'まるまる', 'まるまる', 'まるまる', 'marumaru', 'plump, rotund, chubby', 'HU', 'audio/v8h-marumaru.m4a', 'VISUAL'),
(38, 'がりがり', 'ガリガリ', 'ガリガリ', 'garigari', 'very skinny, emaciated', 'KD', 'audio/v8k-garigari.m4a', 'VISUAL'),
(39, 'むくむく', 'むくむく', 'むくむく', 'mukumuku', 'billowing, plump', 'HU', 'audio/v9h-mukumuku.m4a', 'VISUAL'),
(40, 'ぼさぼさ', 'ボサボサ', 'ボサボサ', 'bosabosa', 'ruffled, disheveled', 'KD', 'audio/v9k-bosabosa.m4a', 'VISUAL'),
(41, 'うんざり', 'うんざり', 'うんざり', 'uNzari', 'boredom, tedious, fed up with', 'HU', 'audio/i0h-uNzari.m4a', 'INTEROCEPTIVE'),
(42, 'わくわく', 'ワクワク', 'ワクワク', 'wakuwaku', 'excitement, nervous', 'KD', 'audio/i0k-wakuwaku.m4a', 'INTEROCEPTIVE'),
(43, 'げんなり', 'げんなり', 'げんなり', 'geNnari', 'weary, fed up, dejected', 'HU', 'audio/i1h-geNnari.m4a', 'INTEROCEPTIVE'),
(44, 'がつがつ', 'ガツガツ', 'ガツガツ', 'gatugatu', 'ravenously, greedily', 'KD', 'audio/i1k-gatugatu.m4a', 'INTEROCEPTIVE'),
(45, 'しょぼん', 'しょぼん', 'しょぼん', 'syoboN', 'downhearted, dejected', 'HU', 'audio/i2h-syoboN.m4a', 'INTEROCEPTIVE'),
(46, 'るんるん', 'ルンルン', 'ルンルン', 'ruNruN', 'happy, elated, euphoric', 'KD', 'audio/i2k-ruNruN.m4a', 'INTEROCEPTIVE'),
(47, 'すっきり', 'すっきり', 'すっきり', 'sukkiri', 'refreshingly, with a feeling of relief', 'HU', 'audio/i3h-sukkiri.m4a', 'INTEROCEPTIVE'),
(48, 'いらいら', 'イライラ', 'イライラ', 'iraira', 'to get irritated, annoyed', 'KD', 'audio/i3k-iraira.m4a', 'INTEROCEPTIVE'),
(49, 'ちゃんと', 'ちゃんと', 'ちゃんと', 'tyaNto', 'attentive, proper attitude', 'HU', 'audio/i4h-tyaNto.m4a', 'INTEROCEPTIVE'),
(50, 'どぎまぎ', 'ドギマギ', 'ドギマギ', 'dogimagi', 'flurried, nervous', 'KD', 'audio/i4k-dogimagi.m4a', 'INTEROCEPTIVE'),
(51, 'のびのび', 'のびのび', 'のびのび', 'nobinobi', 'comfortable, peaceful', 'HU', 'audio/i5h-nobinobi.m4a', 'INTEROCEPTIVE'),
(52, 'はらはら', 'ハラハラ', 'ハラハラ', 'harahara', 'anxious, nervous', 'KD', 'audio/i5k-harahara.m4a', 'INTEROCEPTIVE'),
(53, 'のんびり', 'のんびり', 'のんびり', 'noNbiri', 'at leisure, in a relaxed manner', 'HU', 'audio/i6h-noNbiri.m4a', 'INTEROCEPTIVE'),
(54, 'ぎりぎり', 'ギリギリ', 'ギリギリ', 'girigiri', 'just barely, at the last moment', 'KD', 'audio/i6k-girigiri.m4a', 'INTEROCEPTIVE'),
(55, 'ほっこり', 'ほっこり', 'ほっこり', 'hokkori', 'feeling warm and fluffy, soft', 'HU', 'audio/i7h-hokkori.m4a', 'INTEROCEPTIVE'),
(56, 'ぞくぞく', 'ゾクゾク', 'ゾクゾク', 'zokuzoku', 'shivering, feeling chilly', 'KD', 'audio/i7k-zokuzoku.m4a', 'INTEROCEPTIVE'),
(57, 'ほっ', 'ほっ', 'ほっ', 'hoQ', 'with a feeling of relief', 'HU', 'audio/i8h-hoQ.m4a', 'INTEROCEPTIVE'),
(58, 'どきり', 'ドキリ', 'ドキリ', 'dokiri', 'being startled, getting a shock', 'KD', 'audio/i8k-dokiri.m4a', 'INTEROCEPTIVE'),
(59, 'ゆったり', 'ゆったり', 'ゆったり', 'yuttari', 'comfortable, calm, relaxed', 'HU', 'audio/i9h-yuttari.m4a', 'INTEROCEPTIVE'),
(60, 'どきどき', 'ドキドキ', 'ドキドキ', 'dokidoki', 'with a rapid heartbeat', 'KD', 'audio/i9k-dokidoki.m4a', 'INTEROCEPTIVE'),
(61, 'ごそごそ', 'ごそごそ', 'ごそごそ', 'gosogoso', 'with a rustling sound', 'HH', 'audio/a0h-gosogoso.m4a', 'AUDITORY'),
(62, 'かたかた', 'カタカタ', 'カタカタ', 'katakata', 'clattering, rattling', 'KK', 'audio/a0k-katakata.m4a', 'AUDITORY'),
(63, 'しとしと', 'しとしと', 'しとしと', 'sitosito', 'drizzling', 'HH', 'audio/a1h-sitosito.m4a', 'AUDITORY'),
(64, 'ばちゃばちゃ', 'バチャバチャ', 'バチャバチャ', 'batyabatya', 'splashing', 'KK', 'audio/a1k-batyabatya.m4a', 'AUDITORY'),
(65, 'じゃあじゃあ', 'じゃーじゃー', 'じゃーじゃー', 'zyaazyaa', 'noisily gushing', 'HH', 'audio/a2h-zyaazyaa.m4a', 'AUDITORY'),
(66, 'ぽたぽた', 'ポタポタ', 'ポタポタ', 'potapota', 'dripping, trickling', 'KK', 'audio/a2k-potapota.m4a', 'AUDITORY'),
(67, 'じりじり', 'じりじり', 'じりじり', 'ziriziri', 'sizzling, oozing', 'HH', 'audio/a3h-ziriziri.m4a', 'AUDITORY'),
(68, 'しゃきしゃき', 'シャキシャキ', 'シャキシャキ', 'syakisyaki', 'crisp, crunchy', 'KK', 'audio/a3k-syakisyaki.m4a', 'AUDITORY'),
(69, 'ずるずる', 'ずるずる', 'ずるずる', 'zuruzuru', 'with a slurp', 'HH', 'audio/a4h-zuruzuru.m4a', 'AUDITORY'),
(70, 'ぽりぽり', 'ポリポリ', 'ポリポリ', 'poripori', 'munching, crunching', 'KK', 'audio/a4k-poripori.m4a', 'AUDITORY'),
(71, 'そろりそろり', 'そろりそろり', 'そろりそろり', 'sororisorori', 'slowly and quietly', 'HH', 'audio/a5h-sororisorori.m4a', 'AUDITORY'),
(72, 'どたばた', 'ドタバタ', 'ドタバタ', 'dotabata', 'noisily, with heavy feet', 'KK', 'audio/a5k-dotabata.m4a', 'AUDITORY'),
(73, 'どさり', 'どさり', 'どさり', 'dosari', 'with a thud', 'HH', 'audio/a6h-dosari.m4a', 'AUDITORY'),
(74, 'かちっ', 'カチッ', 'カチッ', 'katiQ', 'click, snap', 'KK', 'audio/a6k-katiQ.m4a', 'AUDITORY'),
(75, 'どしどし', 'どしどし', 'どしどし', 'dosidosi', 'tramping, stomping', 'HH', 'audio/a7h-dosidosi.m4a', 'AUDITORY'),
(76, 'ぎしぎし', 'ギシギシ', 'ギシギシ', 'gisigisi', 'with a creak, squeak', 'KK', 'audio/a7k-gisigisi.m4a', 'AUDITORY'),
(77, 'ぼそり', 'ぼそり', 'ぼそり', 'bosori', 'in a whisper, in a murmur', 'HH', 'audio/a8h-bosori.m4a', 'AUDITORY'),
(78, 'きゃあきゃあ', 'キャーキャー', 'キャーキャー', 'kyaakyaa', 'shrieking, squealing', 'KK', 'audio/a8k-kyaakyaa.m4a', 'AUDITORY'),
(79, 'もごもご', 'もごもご', 'もごもご', 'mogomogo', 'mumbling, chewing', 'HH', 'audio/a9h-mogomogo.m4a', 'AUDITORY'),
(80, 'さくっ', 'サクッ', 'サクッ', 'sakuQ', 'with a crunch', 'KK', 'audio/a9k-sakuQ.m4a', 'AUDITORY'),
(81, 'くっきり', 'くっきり', 'くっきり', 'kukkiri', 'clearly, distinctly, sharply', 'HH', 'audio/v0h-kukkiri.m4a', 'VISUAL'),
(82, 'ちらちら', 'チラチラ', 'チラチラ', 'tiratira', 'flickering, fluttering', 'KK', 'audio/v0k-tiratira.m4a', 'VISUAL'),
(83, 'くりくり', 'くりくり', 'くりくり', 'kurikuri', 'big and round', 'HH', 'audio/v1h-kurikuri.m4a', 'VISUAL'),
(84, 'ぎざぎざ', 'ギザギザ', 'ギザギザ', 'gizagiza', 'jagged, serrated', 'KK', 'audio/v1k-gizagiza.m4a', 'VISUAL'),
(85, 'どんより', 'どんより', 'どんより', 'doNyori', 'dark, gloomy', 'HH', 'audio/v2h-doNyori.m4a', 'VISUAL'),
(86, 'きらきら', 'キラキラ', 'キラキラ', 'kirakira', 'glittering, sparkling', 'KK', 'audio/v2k-kirakira.m4a', 'VISUAL'),
(87, 'ぴたり', 'ぴたり', 'ぴたり', 'pitari', 'tightly connected, no space in between', 'HH', 'audio/v3h-pitari.m4a', 'VISUAL'),
(88, 'ひらひら', 'ヒラヒラ', 'ヒラヒラ', 'hirahira', 'fluttering, dangling', 'KK', 'audio/v3k-hirahira.m4a', 'VISUAL'),
(89, 'びっしり', 'びっしり', 'びっしり', 'bissiri', 'closely lined up, densely', 'HH', 'audio/v4h-bissiri.m4a', 'VISUAL'),
(90, 'ばらばら', 'バラバラ', 'バラバラ', 'barabara', 'scattered, in pieces', 'KK', 'audio/v4k-barabara.m4a', 'VISUAL'),
(91, 'ぶすり', 'ぶすり', 'ぶすり', 'busuri', 'poutingly, displeasure', 'HH', 'audio/v5h-busuri.m4a', 'VISUAL'),
(92, 'にこにこ', 'ニコニコ', 'ニコニコ', 'nikoniko', 'smilingly, with a grin', 'KK', 'audio/v5k-nikoniko.m4a', 'VISUAL'),
(93, 'ふんわり', 'ふんわり', 'ふんわり', 'huNwari', 'gently, airily, fluffy', 'HH', 'audio/v6h-huNwari.m4a', 'VISUAL'),
(94, 'ごつごつ', 'ゴツゴツ', 'ゴツゴツ', 'gotugotu', 'rugged, scragged, angular', 'KK', 'audio/v6k-gotugotu.m4a', 'VISUAL'),
(95, 'ぼんやり', 'ぼんやり', 'ぼんやり', 'boNyari', 'dim, faint, indistinct', 'HH', 'audio/v7h-boNyari.m4a', 'VISUAL'),
(96, 'きりっ', 'キリッ', 'キリッ', 'kiriQ', 'crisp appearance, stiffly', 'KK', 'audio/v7k-kiriQ.m4a', 'VISUAL'),
(97, 'まるまる', 'まるまる', 'まるまる', 'marumaru', 'plump, rotund, chubby', 'HH', 'audio/v8h-marumaru.m4a', 'VISUAL'),
(98, 'がりがり', 'ガリガリ', 'ガリガリ', 'garigari', 'very skinny, emaciated', 'KK', 'audio/v8k-garigari.m4a', 'VISUAL'),
(99, 'むくむく', 'むくむく', 'むくむく', 'mukumuku', 'billowing, plump', 'HH', 'audio/v9h-mukumuku.m4a', 'VISUAL'),
(100, 'ぼさぼさ', 'ボサボサ', 'ボサボサ', 'bosabosa', 'ruffled, disheveled', 'KK', 'audio/v9k-bosabosa.m4a', 'VISUAL'),
(101, 'うんざり', 'うんざり', 'うんざり', 'uNzari', 'boredom, tedious, fed up with', 'HH', 'audio/i0h-uNzari.m4a', 'INTEROCEPTIVE'),
(102, 'わくわく', 'ワクワク', 'ワクワク', 'wakuwaku', 'excitement, nervous', 'KK', 'audio/i0k-wakuwaku.m4a', 'INTEROCEPTIVE'),
(103, 'げんなり', 'げんなり', 'げんなり', 'geNnari', 'weary, fed up, dejected', 'HH', 'audio/i1h-geNnari.m4a', 'INTEROCEPTIVE'),
(104, 'がつがつ', 'ガツガツ', 'ガツガツ', 'gatugatu', 'ravenously, greedily', 'KK', 'audio/i1k-gatugatu.m4a', 'INTEROCEPTIVE'),
(105, 'しょぼん', 'しょぼん', 'しょぼん', 'syoboN', 'downhearted, dejected', 'HH', 'audio/i2h-syoboN.m4a', 'INTEROCEPTIVE'),
(106, 'るんるん', 'ルンルン', 'ルンルン', 'ruNruN', 'happy, elated, euphoric', 'KK', 'audio/i2k-ruNruN.m4a', 'INTEROCEPTIVE'),
(107, 'すっきり', 'すっきり', 'すっきり', 'sukkiri', 'refreshingly, with a feeling of relief', 'HH', 'audio/i3h-sukkiri.m4a', 'INTEROCEPTIVE'),
(108, 'いらいら', 'イライラ', 'イライラ', 'iraira', 'to get irritated, annoyed', 'KK', 'audio/i3k-iraira.m4a', 'INTEROCEPTIVE'),
(109, 'ちゃんと', 'ちゃんと', 'ちゃんと', 'tyaNto', 'attentive, proper attitude', 'HH', 'audio/i4h-tyaNto.m4a', 'INTEROCEPTIVE'),
(110, 'どぎまぎ', 'ドギマギ', 'ドギマギ', 'dogimagi', 'flurried, nervous', 'KK', 'audio/i4k-dogimagi.m4a', 'INTEROCEPTIVE'),
(111, 'のびのび', 'のびのび', 'のびのび', 'nobinobi', 'comfortable, peaceful', 'HH', 'audio/i5h-nobinobi.m4a', 'INTEROCEPTIVE'),
(112, 'はらはら', 'ハラハラ', 'ハラハラ', 'harahara', 'anxious, nervous', 'KK', 'audio/i5k-harahara.m4a', 'INTEROCEPTIVE'),
(113, 'のんびり', 'のんびり', 'のんびり', 'noNbiri', 'at leisure, in a relaxed manner', 'HH', 'audio/i6h-noNbiri.m4a', 'INTEROCEPTIVE'),
(114, 'ぎりぎり', 'ギリギリ', 'ギリギリ', 'girigiri', 'just barely, at the last moment', 'KK', 'audio/i6k-girigiri.m4a', 'INTEROCEPTIVE'),
(115, 'ほっこり', 'ほっこり', 'ほっこり', 'hokkori', 'feeling warm and fluffy, soft', 'HH', 'audio/i7h-hokkori.m4a', 'INTEROCEPTIVE'),
(116, 'ぞくぞく', 'ゾクゾク', 'ゾクゾク', 'zokuzoku', 'shivering, feeling chilly', 'KK', 'audio/i7k-zokuzoku.m4a', 'INTEROCEPTIVE'),
(117, 'ほっ', 'ほっ', 'ほっ', 'hoQ', 'with a feeling of relief', 'HH', 'audio/i8h-hoQ.m4a', 'INTEROCEPTIVE'),
(118, 'どきり', 'ドキリ', 'ドキリ', 'dokiri', 'being startled, getting a shock', 'KK', 'audio/i8k-dokiri.m4a', 'INTEROCEPTIVE'),
(119, 'ゆったり', 'ゆったり', 'ゆったり', 'yuttari', 'comfortable, calm, relaxed', 'HH', 'audio/i9h-yuttari.m4a', 'INTEROCEPTIVE'),
(120, 'どきどき', 'ドキドキ', 'ドキドキ', 'dokidoki', 'with a rapid heartbeat', 'KK', 'audio/i9k-dokidoki.m4a', 'INTEROCEPTIVE'),
(121, 'ごそごそ', 'ゴソゴソ', 'ごそごそ', 'gosogoso', 'with a rustling sound', 'HK', 'audio/a0h-gosogoso.m4a', 'AUDITORY'),
(122, 'かたかた', 'かたかた', 'カタカタ', 'katakata', 'clattering, rattling', 'KH', 'audio/a0k-katakata.m4a', 'AUDITORY'),
(123, 'しとしと', 'シトシト', 'しとしと', 'sitosito', 'drizzling', 'HK', 'audio/a1h-sitosito.m4a', 'AUDITORY'),
(124, 'ばちゃばちゃ', 'ばちゃばちゃ', 'バチャバチャ', 'batyabatya', 'splashing', 'KH', 'audio/a1k-batyabatya.m4a', 'AUDITORY'),
(125, 'じゃあじゃあ', 'ジャージャー', 'じゃーじゃー', 'zyaazyaa', 'noisily gushing', 'HK', 'audio/a2h-zyaazyaa.m4a', 'AUDITORY'),
(126, 'ぽたぽた', 'ぽたぽた', 'ポタポタ', 'potapota', 'dripping, trickling', 'KH', 'audio/a2k-potapota.m4a', 'AUDITORY'),
(127, 'じりじり', 'ジリジリ', 'じりじり', 'ziriziri', 'sizzling, oozing', 'HK', 'audio/a3h-ziriziri.m4a', 'AUDITORY'),
(128, 'しゃきしゃき', 'しゃきしゃき', 'シャキシャキ', 'syakisyaki', 'crisp, crunchy', 'KH', 'audio/a3k-syakisyaki.m4a', 'AUDITORY'),
(129, 'ずるずる', 'ズルズル', 'ずるずる', 'zuruzuru', 'with a slurp', 'HK', 'audio/a4h-zuruzuru.m4a', 'AUDITORY'),
(130, 'ぽりぽり', 'ぽりぽり', 'ポリポリ', 'poripori', 'munching, crunching', 'KH', 'audio/a4k-poripori.m4a', 'AUDITORY'),
(131, 'そろりそろり', 'ソロリソロリ', 'そろりそろり', 'sororisorori', 'slowly and quietly', 'HK', 'audio/a5h-sororisorori.m4a', 'AUDITORY'),
(132, 'どたばた', 'どたばた', 'ドタバタ', 'dotabata', 'noisily, with heavy feet', 'KH', 'audio/a5k-dotabata.m4a', 'AUDITORY'),
(133, 'どさり', 'ドサリ', 'どさり', 'dosari', 'with a thud', 'HK', 'audio/a6h-dosari.m4a', 'AUDITORY'),
(134, 'かちっ', 'かちっ', 'カチッ', 'katiQ', 'click, snap', 'KH', 'audio/a6k-katiQ.m4a', 'AUDITORY'),
(135, 'どしどし', 'ドシドシ', 'どしどし', 'dosidosi', 'tramping, stomping', 'HK', 'audio/a7h-dosidosi.m4a', 'AUDITORY'),
(136, 'ぎしぎし', 'ぎしぎし', 'ギシギシ', 'gisigisi', 'with a creak, squeak', 'KH', 'audio/a7k-gisigisi.m4a', 'AUDITORY'),
(137, 'ぼそり', 'ボソリ', 'ぼそり', 'bosori', 'in a whisper, in a murmur', 'HK', 'audio/a8h-bosori.m4a', 'AUDITORY'),
(138, 'きゃあきゃあ', 'きゃーきゃー', 'キャーキャー', 'kyaakyaa', 'shrieking, squealing', 'KH', 'audio/a8k-kyaakyaa.m4a', 'AUDITORY'),
(139, 'もごもご', 'モゴモゴ', 'もごもご', 'mogomogo', 'mumbling, chewing', 'HK', 'audio/a9h-mogomogo.m4a', 'AUDITORY'),
(140, 'さくっ', 'さくっ', 'サクッ', 'sakuQ', 'with a crunch', 'KH', 'audio/a9k-sakuQ.m4a', 'AUDITORY'),
(141, 'くっきり', 'クッキリ', 'くっきり', 'kukkiri', 'clearly, distinctly, sharply', 'HK', 'audio/v0h-kukkiri.m4a', 'VISUAL'),
(142, 'ちらちら', 'ちらちら', 'チラチラ', 'tiratira', 'flickering, fluttering', 'KH', 'audio/v0k-tiratira.m4a', 'VISUAL'),
(143, 'くりくり', 'クリクリ', 'くりくり', 'kurikuri', 'big and round', 'HK', 'audio/v1h-kurikuri.m4a', 'VISUAL'),
(144, 'ぎざぎざ', 'ぎざぎざ', 'ギザギザ', 'gizagiza', 'jagged, serrated', 'KH', 'audio/v1k-gizagiza.m4a', 'VISUAL'),
(145, 'どんより', 'ドンヨリ', 'どんより', 'doNyori', 'dark, gloomy', 'HK', 'audio/v2h-doNyori.m4a', 'VISUAL'),
(146, 'きらきら', 'きらきら', 'キラキラ', 'kirakira', 'glittering, sparkling', 'KH', 'audio/v2k-kirakira.m4a', 'VISUAL'),
(147, 'ぴたり', 'ピタリ', 'ぴたり', 'pitari', 'tightly connected, no space in between', 'HK', 'audio/v3h-pitari.m4a', 'VISUAL'),
(148, 'ひらひら', 'ひらひら', 'ヒラヒラ', 'hirahira', 'fluttering, dangling', 'KH', 'audio/v3k-hirahira.m4a', 'VISUAL'),
(149, 'びっしり', 'ビッシリ', 'びっしり', 'bissiri', 'closely lined up, densely', 'HK', 'audio/v4h-bissiri.m4a', 'VISUAL'),
(150, 'ばらばら', 'ばらばら', 'バラバラ', 'barabara', 'scattered, in pieces', 'KH', 'audio/v4k-barabara.m4a', 'VISUAL'),
(151, 'ぶすり', 'ブスリ', 'ぶすり', 'busuri', 'poutingly, displeasure', 'HK', 'audio/v5h-busuri.m4a', 'VISUAL'),
(152, 'にこにこ', 'にこにこ', 'ニコニコ', 'nikoniko', 'smilingly, with a grin', 'KH', 'audio/v5k-nikoniko.m4a', 'VISUAL'),
(153, 'ふんわり', 'フンワリ', 'ふんわり', 'huNwari', 'gently, airily, fluffy', 'HK', 'audio/v6h-huNwari.m4a', 'VISUAL'),
(154, 'ごつごつ', 'ごつごつ', 'ゴツゴツ', 'gotugotu', 'rugged, scragged, angular', 'KH', 'audio/v6k-gotugotu.m4a', 'VISUAL'),
(155, 'ぼんやり', 'ボンヤリ', 'ぼんやり', 'boNyari', 'dim, faint, indistinct', 'HK', 'audio/v7h-boNyari.m4a', 'VISUAL'),
(156, 'きりっ', 'きりっ', 'キリッ', 'kiriQ', 'crisp appearance, stiffly', 'KH', 'audio/v7k-kiriQ.m4a', 'VISUAL'),
(157, 'まるまる', 'マルマル', 'まるまる', 'marumaru', 'plump, rotund, chubby', 'HK', 'audio/v8h-marumaru.m4a', 'VISUAL'),
(158, 'がりがり', 'がりがり', 'ガリガリ', 'garigari', 'very skinny, emaciated', 'KH', 'audio/v8k-garigari.m4a', 'VISUAL'),
(159, 'むくむく', 'ムクムク', 'むくむく', 'mukumuku', 'billowing, plump', 'HK', 'audio/v9h-mukumuku.m4a', 'VISUAL'),
(160, 'ぼさぼさ', 'ぼさぼさ', 'ボサボサ', 'bosabosa', 'ruffled, disheveled', 'KH', 'audio/v9k-bosabosa.m4a', 'VISUAL'),
(161, 'うんざり', 'ウンザリ', 'うんざり', 'uNzari', 'boredom, tedious, fed up with', 'HK', 'audio/i0h-uNzari.m4a', 'INTEROCEPTIVE'),
(162, 'わくわく', 'わくわく', 'ワクワク', 'wakuwaku', 'excitement, nervous', 'KH', 'audio/i0k-wakuwaku.m4a', 'INTEROCEPTIVE'),
(163, 'げんなり', 'ゲンナリ', 'げんなり', 'geNnari', 'weary, fed up, dejected', 'HK', 'audio/i1h-geNnari.m4a', 'INTEROCEPTIVE'),
(164, 'がつがつ', 'がつがつ', 'ガツガツ', 'gatugatu', 'ravenously, greedily', 'KH', 'audio/i1k-gatugatu.m4a', 'INTEROCEPTIVE'),
(165, 'しょぼん', 'ショボン', 'しょぼん', 'syoboN', 'downhearted, dejected', 'HK', 'audio/i2h-syoboN.m4a', 'INTEROCEPTIVE'),
(166, 'るんるん', 'るんるん', 'ルンルン', 'ruNruN', 'happy, elated, euphoric', 'KH', 'audio/i2k-ruNruN.m4a', 'INTEROCEPTIVE'),
(167, 'すっきり', 'スッキリ', 'すっきり', 'sukkiri', 'refreshingly, with a feeling of relief', 'HK', 'audio/i3h-sukkiri.m4a', 'INTEROCEPTIVE'),
(168, 'いらいら', 'いらいら', 'イライラ', 'iraira', 'to get irritated, annoyed', 'KH', 'audio/i3k-iraira.m4a', 'INTEROCEPTIVE'),
(169, 'ちゃんと', 'チャント', 'ちゃんと', 'tyaNto', 'attentive, proper attitude', 'HK', 'audio/i4h-tyaNto.m4a', 'INTEROCEPTIVE'),
(170, 'どぎまぎ', 'どぎまぎ', 'ドギマギ', 'dogimagi', 'flurried, nervous', 'KH', 'audio/i4k-dogimagi.m4a', 'INTEROCEPTIVE'),
(171, 'のびのび', 'ノビノビ', 'のびのび', 'nobinobi', 'comfortable, peaceful', 'HK', 'audio/i5h-nobinobi.m4a', 'INTEROCEPTIVE'),
(172, 'はらはら', 'はらはら', 'ハラハラ', 'harahara', 'anxious, nervous', 'KH', 'audio/i5k-harahara.m4a', 'INTEROCEPTIVE'),
(173, 'のんびり', 'ノンビリ', 'のんびり', 'noNbiri', 'at leisure, in a relaxed manner', 'HK', 'audio/i6h-noNbiri.m4a', 'INTEROCEPTIVE'),
(174, 'ぎりぎり', 'ぎりぎり', 'ギリギリ', 'girigiri', 'just barely, at the last moment', 'KH', 'audio/i6k-girigiri.m4a', 'INTEROCEPTIVE'),
(175, 'ほっこり', 'ホッコリ', 'ほっこり', 'hokkori', 'feeling warm and fluffy, soft', 'HK', 'audio/i7h-hokkori.m4a', 'INTEROCEPTIVE'),
(176, 'ぞくぞく', 'ぞくぞく', 'ゾクゾク', 'zokuzoku', 'shivering, feeling chilly', 'KH', 'audio/i7k-zokuzoku.m4a', 'INTEROCEPTIVE'),
(177, 'ほっ', 'ホッ', 'ほっ', 'hoQ', 'with a feeling of relief', 'HK', 'audio/i8h-hoQ.m4a', 'INTEROCEPTIVE'),
(178, 'どきり', 'どきり', 'ドキリ', 'dokiri', 'being startled, getting a shock', 'KH', 'audio/i8k-dokiri.m4a', 'INTEROCEPTIVE'),
(179, 'ゆったり', 'ユッタリ', 'ゆったり', 'yuttari', 'comfortable, calm, relaxed', 'HK', 'audio/i9h-yuttari.m4a', 'INTEROCEPTIVE'),
(180, 'どきどき', 'どきどき', 'ドキドキ', 'dokidoki', 'with a rapid heartbeat', 'KH', 'audio/i9k-dokidoki.m4a', 'INTEROCEPTIVE'),
(181, 'そっと', 'そっと', 'そっと', 'sotto', 'softly, gently', 'HU', 'audio/p0h-sotto.m4a', 'AUDITORY'),
(182, 'がたん', 'ガタン', 'ガタン', 'gataN', 'with a bang', 'KD', 'audio/p0k-gataN.m4a', 'AUDITORY'),
(183, 'じっと', 'じっと', 'じっと', 'zitto', 'motionless, fixedly', 'HU', 'audio/p1h-zitto.m4a', 'VISUAL'),
(184, 'ぱっ', 'パッ', 'パッ', 'paQ', 'suddenly, in a flash', 'KD', 'audio/p1k-paQ.m4a', 'VISUAL'),
(185, 'そろそろ', 'そろそろ', 'そろそろ', 'sorosoro', 'slowly, quietly', 'HU', 'audio/p2h-sorosoro.m4a', 'AUDITORY'),
(186, 'がんがん', 'ガンガン', 'ガンガン', 'gaNgaN', 'clanging, banging', 'KD', 'audio/p2k-gaNgaN.m4a', 'AUDITORY'),
(187, 'そっくり', 'そっくり', 'そっくり', 'sokkuri', 'exactly alike, spitting image', 'HU', 'audio/p3h-sokkuri.m4a', 'VISUAL'),
(188, 'がらり', 'ガラリ', 'ガラリ', 'garari', 'completely, totally changed', 'KD', 'audio/p3k-garari.m4a', 'VISUAL'),
(189, 'そっと', 'そっと', 'そっと', 'sotto', 'softly, gently', 'HH', 'audio/p0h-sotto.m4a', 'AUDITORY'),
(190, 'がたん', 'ガタン', 'ガタン', 'gataN', 'with a bang', 'KK', 'audio/p0k-gataN.m4a', 'AUDITORY'),
(191, 'じっと', 'じっと', 'じっと', 'zitto', 'motionless, fixedly', 'HH', 'audio/p1h-zitto.m4a', 'VISUAL'),
(192, 'ぱっ', 'パッ', 'パッ', 'paQ', 'suddenly, in a flash', 'KK', 'audio/p1k-paQ.m4a', 'VISUAL'),
(193, 'そろそろ', 'そろそろ', 'そろそろ', 'sorosoro', 'slowly, quietly', 'HH', 'audio/p2h-sorosoro.m4a', 'AUDITORY'),
(194, 'がんがん', 'ガンガン', 'ガンガン', 'gaNgaN', 'clanging, banging', 'KK', 'audio/p2k-gaNgaN.m4a', 'AUDITORY'),
(195, 'そっくり', 'そっくり', 'そっくり', 'sokkuri', 'exactly alike, spitting image', 'HH', 'audio/p3h-sokkuri.m4a', 'VISUAL'),
(196, 'がらり', 'ガラリ', 'ガラリ', 'garari', 'completely, totally changed', 'KK', 'audio/p3k-garari.m4a', 'VISUAL'),
(197, 'そっと', 'ソット', 'そっと', 'sotto', 'softly, gently', 'HK', 'audio/p0h-sotto.m4a', 'AUDITORY'),
(198, 'がたん', 'がたん', 'ガタン', 'gataN', 'with a bang', 'KH', 'audio/p0k-gataN.m4a', 'AUDITORY'),
(199, 'じっと', 'ジット', 'じっと', 'zitto', 'motionless, fixedly', 'HK', 'audio/p1h-zitto.m4a', 'VISUAL'),
(200, 'ぱっ', 'ぱっ', 'パッ', 'paQ', 'suddenly, in a flash', 'KH', 'audio/p1k-paQ.m4a', 'VISUAL'),
(201, 'そろそろ', 'ソロソロ', 'そろそろ', 'sorosoro', 'slowly, quietly', 'HK', 'audio/p2h-sorosoro.m4a', 'AUDITORY'),
(202, 'がんがん', 'がんがん', 'ガンガン', 'gaNgaN', 'clanging, banging', 'KH', 'audio/p2k-gaNgaN.m4a', 'AUDITORY'),
(203, 'そっくり', 'ソックリ', 'そっくり', 'sokkuri', 'exactly alike, spitting image', 'HK', 'audio/p3h-sokkuri.m4a', 'VISUAL'),
(204, 'がらり', 'がらり', 'ガラリ', 'garari', 'completely, totally changed', 'KH', 'audio/p3k-garari.m4a', 'VISUAL');

INSERT INTO arena_rounds (id, prompt, left_ideophone_id, right_ideophone_id, correct_ideophone_id, condition_name, difficulty_level, is_practice)
VALUES
(1, 'with a rustling sound', 1, 2, 1, 'CONDITION_1_SOKUON', 1, 0),
(2, 'splashing', 3, 4, 4, 'CONDITION_1_SOKUON', 1, 0),
(3, 'noisily gushing', 5, 6, 5, 'CONDITION_1_SOKUON', 1, 0),
(4, 'crisp, crunchy', 7, 8, 8, 'CONDITION_1_SOKUON', 1, 0),
(5, 'with a slurp', 9, 10, 9, 'CONDITION_1_SOKUON', 1, 0),
(6, 'noisily, with heavy feet', 11, 12, 12, 'CONDITION_1_SOKUON', 1, 0),
(7, 'with a thud', 13, 14, 13, 'CONDITION_1_SOKUON', 1, 0),
(8, 'with a creak, squeak', 15, 16, 16, 'CONDITION_1_SOKUON', 1, 0),
(9, 'in a whisper, in a murmur', 17, 18, 17, 'CONDITION_1_SOKUON', 1, 0),
(10, 'with a crunch', 19, 20, 20, 'CONDITION_1_SOKUON', 1, 0),
(11, 'clearly, distinctly, sharply', 21, 22, 21, 'CONDITION_1_SOKUON', 1, 0),
(12, 'jagged, serrated', 23, 24, 24, 'CONDITION_1_SOKUON', 1, 0),
(13, 'dark, gloomy', 25, 26, 25, 'CONDITION_1_SOKUON', 1, 0),
(14, 'fluttering, dangling', 27, 28, 28, 'CONDITION_1_SOKUON', 1, 0),
(15, 'closely lined up, densely', 29, 30, 29, 'CONDITION_1_SOKUON', 1, 0),
(16, 'smilingly, with a grin', 31, 32, 32, 'CONDITION_1_SOKUON', 1, 0),
(17, 'gently, airily, fluffy', 33, 34, 33, 'CONDITION_1_SOKUON', 1, 0),
(18, 'crisp appearance, stiffly', 35, 36, 36, 'CONDITION_1_SOKUON', 1, 0),
(19, 'plump, rotund, chubby', 37, 38, 37, 'CONDITION_1_SOKUON', 1, 0),
(20, 'ruffled, disheveled', 39, 40, 40, 'CONDITION_1_SOKUON', 1, 0),
(21, 'boredom, tedious, fed up with', 41, 42, 41, 'CONDITION_1_SOKUON', 1, 0),
(22, 'ravenously, greedily', 43, 44, 44, 'CONDITION_1_SOKUON', 1, 0),
(23, 'downhearted, dejected', 45, 46, 45, 'CONDITION_1_SOKUON', 1, 0),
(24, 'to get irritated, annoyed', 47, 48, 48, 'CONDITION_1_SOKUON', 1, 0),
(25, 'attentive, proper attitude', 49, 50, 49, 'CONDITION_1_SOKUON', 1, 0),
(26, 'anxious, nervous', 51, 52, 52, 'CONDITION_1_SOKUON', 1, 0),
(27, 'at leisure, in a relaxed manner', 53, 54, 53, 'CONDITION_1_SOKUON', 1, 0),
(28, 'shivering, feeling chilly', 55, 56, 56, 'CONDITION_1_SOKUON', 1, 0),
(29, 'with a feeling of relief', 57, 58, 57, 'CONDITION_1_SOKUON', 1, 0),
(30, 'with a rapid heartbeat', 59, 60, 60, 'CONDITION_1_SOKUON', 1, 0),
(31, 'with a rustling sound', 61, 62, 61, 'CONDITION_2_SOKUON', 1, 0),
(32, 'splashing', 63, 64, 64, 'CONDITION_2_SOKUON', 1, 0),
(33, 'noisily gushing', 65, 66, 65, 'CONDITION_2_SOKUON', 1, 0),
(34, 'crisp, crunchy', 67, 68, 68, 'CONDITION_2_SOKUON', 1, 0),
(35, 'with a slurp', 69, 70, 69, 'CONDITION_2_SOKUON', 1, 0),
(36, 'noisily, with heavy feet', 71, 72, 72, 'CONDITION_2_SOKUON', 1, 0),
(37, 'with a thud', 73, 74, 73, 'CONDITION_2_SOKUON', 1, 0),
(38, 'with a creak, squeak', 75, 76, 76, 'CONDITION_2_SOKUON', 1, 0),
(39, 'in a whisper, in a murmur', 77, 78, 77, 'CONDITION_2_SOKUON', 1, 0),
(40, 'with a crunch', 79, 80, 80, 'CONDITION_2_SOKUON', 1, 0),
(41, 'clearly, distinctly, sharply', 81, 82, 81, 'CONDITION_2_SOKUON', 1, 0),
(42, 'jagged, serrated', 83, 84, 84, 'CONDITION_2_SOKUON', 1, 0),
(43, 'dark, gloomy', 85, 86, 85, 'CONDITION_2_SOKUON', 1, 0),
(44, 'fluttering, dangling', 87, 88, 88, 'CONDITION_2_SOKUON', 1, 0),
(45, 'closely lined up, densely', 89, 90, 89, 'CONDITION_2_SOKUON', 1, 0),
(46, 'smilingly, with a grin', 91, 92, 92, 'CONDITION_2_SOKUON', 1, 0),
(47, 'gently, airily, fluffy', 93, 94, 93, 'CONDITION_2_SOKUON', 1, 0),
(48, 'crisp appearance, stiffly', 95, 96, 96, 'CONDITION_2_SOKUON', 1, 0),
(49, 'plump, rotund, chubby', 97, 98, 97, 'CONDITION_2_SOKUON', 1, 0),
(50, 'ruffled, disheveled', 99, 100, 100, 'CONDITION_2_SOKUON', 1, 0),
(51, 'boredom, tedious, fed up with', 101, 102, 101, 'CONDITION_2_SOKUON', 1, 0),
(52, 'ravenously, greedily', 103, 104, 104, 'CONDITION_2_SOKUON', 1, 0),
(53, 'downhearted, dejected', 105, 106, 105, 'CONDITION_2_SOKUON', 1, 0),
(54, 'to get irritated, annoyed', 107, 108, 108, 'CONDITION_2_SOKUON', 1, 0),
(55, 'attentive, proper attitude', 109, 110, 109, 'CONDITION_2_SOKUON', 1, 0),
(56, 'anxious, nervous', 111, 112, 112, 'CONDITION_2_SOKUON', 1, 0),
(57, 'at leisure, in a relaxed manner', 113, 114, 113, 'CONDITION_2_SOKUON', 1, 0),
(58, 'shivering, feeling chilly', 115, 116, 116, 'CONDITION_2_SOKUON', 1, 0),
(59, 'with a feeling of relief', 117, 118, 117, 'CONDITION_2_SOKUON', 1, 0),
(60, 'with a rapid heartbeat', 119, 120, 120, 'CONDITION_2_SOKUON', 1, 0),
(61, 'with a rustling sound', 121, 122, 121, 'CONDITION_3_SOKUON', 1, 0),
(62, 'splashing', 123, 124, 124, 'CONDITION_3_SOKUON', 1, 0),
(63, 'noisily gushing', 125, 126, 125, 'CONDITION_3_SOKUON', 1, 0),
(64, 'crisp, crunchy', 127, 128, 128, 'CONDITION_3_SOKUON', 1, 0),
(65, 'with a slurp', 129, 130, 129, 'CONDITION_3_SOKUON', 1, 0),
(66, 'noisily, with heavy feet', 131, 132, 132, 'CONDITION_3_SOKUON', 1, 0),
(67, 'with a thud', 133, 134, 133, 'CONDITION_3_SOKUON', 1, 0),
(68, 'with a creak, squeak', 135, 136, 136, 'CONDITION_3_SOKUON', 1, 0),
(69, 'in a whisper, in a murmur', 137, 138, 137, 'CONDITION_3_SOKUON', 1, 0),
(70, 'with a crunch', 139, 140, 140, 'CONDITION_3_SOKUON', 1, 0),
(71, 'clearly, distinctly, sharply', 141, 142, 141, 'CONDITION_3_SOKUON', 1, 0),
(72, 'jagged, serrated', 143, 144, 144, 'CONDITION_3_SOKUON', 1, 0),
(73, 'dark, gloomy', 145, 146, 145, 'CONDITION_3_SOKUON', 1, 0),
(74, 'fluttering, dangling', 147, 148, 148, 'CONDITION_3_SOKUON', 1, 0),
(75, 'closely lined up, densely', 149, 150, 149, 'CONDITION_3_SOKUON', 1, 0),
(76, 'smilingly, with a grin', 151, 152, 152, 'CONDITION_3_SOKUON', 1, 0),
(77, 'gently, airily, fluffy', 153, 154, 153, 'CONDITION_3_SOKUON', 1, 0),
(78, 'crisp appearance, stiffly', 155, 156, 156, 'CONDITION_3_SOKUON', 1, 0),
(79, 'plump, rotund, chubby', 157, 158, 157, 'CONDITION_3_SOKUON', 1, 0),
(80, 'ruffled, disheveled', 159, 160, 160, 'CONDITION_3_SOKUON', 1, 0),
(81, 'boredom, tedious, fed up with', 161, 162, 161, 'CONDITION_3_SOKUON', 1, 0),
(82, 'ravenously, greedily', 163, 164, 164, 'CONDITION_3_SOKUON', 1, 0),
(83, 'downhearted, dejected', 165, 166, 165, 'CONDITION_3_SOKUON', 1, 0),
(84, 'to get irritated, annoyed', 167, 168, 168, 'CONDITION_3_SOKUON', 1, 0),
(85, 'attentive, proper attitude', 169, 170, 169, 'CONDITION_3_SOKUON', 1, 0),
(86, 'anxious, nervous', 171, 172, 172, 'CONDITION_3_SOKUON', 1, 0),
(87, 'at leisure, in a relaxed manner', 173, 174, 173, 'CONDITION_3_SOKUON', 1, 0),
(88, 'shivering, feeling chilly', 175, 176, 176, 'CONDITION_3_SOKUON', 1, 0),
(89, 'with a feeling of relief', 177, 178, 177, 'CONDITION_3_SOKUON', 1, 0),
(90, 'with a rapid heartbeat', 179, 180, 180, 'CONDITION_3_SOKUON', 1, 0),
(91, 'softly, gently', 181, 182, 181, 'CONDITION_1_SOKUON', 1, 1),
(92, 'suddenly, in a flash', 183, 184, 184, 'CONDITION_1_SOKUON', 1, 1),
(93, 'clanging, banging', 185, 186, 186, 'CONDITION_1_SOKUON', 1, 1),
(94, 'exactly alike, spitting image', 187, 188, 187, 'CONDITION_1_SOKUON', 1, 1),
(95, 'softly, gently', 189, 190, 189, 'CONDITION_2_SOKUON', 1, 1),
(96, 'suddenly, in a flash', 191, 192, 192, 'CONDITION_2_SOKUON', 1, 1),
(97, 'clanging, banging', 193, 194, 194, 'CONDITION_2_SOKUON', 1, 1),
(98, 'exactly alike, spitting image', 195, 196, 195, 'CONDITION_2_SOKUON', 1, 1),
(99, 'softly, gently', 197, 198, 197, 'CONDITION_3_SOKUON', 1, 1),
(100, 'suddenly, in a flash', 199, 200, 200, 'CONDITION_3_SOKUON', 1, 1),
(101, 'clanging, banging', 201, 202, 202, 'CONDITION_3_SOKUON', 1, 1),
(102, 'exactly alike, spitting image', 203, 204, 203, 'CONDITION_3_SOKUON', 1, 1);

-- Dev-only admin account. Throwaway password; see docs/demo-runbook.md, "Creating an admin".
INSERT INTO app_users (id, username, email, password_hash, role)
VALUES
(1, 'arena_admin', 'arena_admin@example.invalid', '$2a$10$AWmwnu11Xi/MVcBlbRLB8OUYrJ7kmfjW9Qzy6tCAk38/Kw0EUGzaK', 'ROLE_ADMIN');
