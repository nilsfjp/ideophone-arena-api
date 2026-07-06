package io.github.nilsfjp.ideophonearena.seed;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Guards the experiment invariants of the M2 word-grain seed: word identity is
 * an entity (68 words) with its script manipulations split into presentations
 * (204), the condition dimension collapses into pairings/trials (34 each), and
 * the thesis backfill and per-word audio (invariant 2) are correct. Parses the
 * generated SQL directly so the schema owner (generate_seed_sql.py) stays the
 * source of truth.
 */
class IdeophoneSeedIntegrityTests {

    private static final Path SEED_PATH =
            Path.of("src/main/resources/db/init/ideophone_arena.sql");

    private static final Pattern AUDIO_PATTERN = Pattern.compile(
            "audio/([avip])(\\d)([hk])-([A-Za-z]+)\\.m4a");

    private static final Map<String, String> MODALITY_BY_LETTER = Map.of(
            "a", "AUDITORY",
            "v", "VISUAL",
            "i", "INTEROCEPTIVE");

    private static final Set<String> CONDITIONS = Set.of(
            "CONDITION_1_SOKUON", "CONDITION_2_SOKUON", "CONDITION_3_SOKUON");

    private record Word(long id, long languageId, String romaji, String kana, String canonicalForm,
            String canonicalScript, String gloss, String modality, String semanticCategory, String stimulusFile) {
    }

    private record Presentation(long id, long wordId, String conditionName, String displayForm, String scriptCode) {
    }

    private record PairingRow(long id, String pairCode, long languageId, long wordAId, long wordBId, String modality,
            boolean core, String source, String thesisAccuracy) {
    }

    private record TrialRow(long id, String roundType, long pairingId, Long correctWordId, boolean practice) {
    }

    private record UserRow(long id, String username, String email, String role) {
    }

    private record SessionRow(long id, String sessionUuid, long userId, String conditionName, long shuffleSeed) {
    }

    private record AnswerRow(long id, long sessionId, long trialId, long selectedWordId, long targetWordId,
            boolean correct, Long responseTimeMs) {
    }

    private record RatingRow(long id, long userId, long wordId, Long sessionId, int rating, Long responseTimeMs) {
    }

    private static String seedSql;
    private static List<Word> words;
    private static List<Presentation> presentations;
    private static List<PairingRow> pairings;
    private static List<TrialRow> trials;
    private static List<UserRow> appUsers;
    private static List<SessionRow> sessions;
    private static List<AnswerRow> answers;
    private static List<RatingRow> ratings;
    private static Map<Long, Word> wordsById;

    @BeforeAll
    static void parseSeed() throws IOException {
        String sql = Files.readString(SEED_PATH, StandardCharsets.UTF_8);
        seedSql = sql;

        words = new ArrayList<>();
        for (List<String> f : rows(sql, "words")) {
            words.add(new Word(l(f, 0), l(f, 1), s(f, 2), s(f, 3), s(f, 4), s(f, 5), s(f, 6),
                    s(f, 7), s(f, 8), s(f, 9)));
        }
        presentations = new ArrayList<>();
        for (List<String> f : rows(sql, "presentations")) {
            presentations.add(new Presentation(l(f, 0), l(f, 1), s(f, 2), s(f, 3), s(f, 4)));
        }
        pairings = new ArrayList<>();
        for (List<String> f : rows(sql, "pairings")) {
            pairings.add(new PairingRow(l(f, 0), s(f, 1), l(f, 2), l(f, 3), l(f, 4), s(f, 5),
                    "1".equals(f.get(6)), s(f, 7), f.get(9)));
        }
        trials = new ArrayList<>();
        for (List<String> f : rows(sql, "trials")) {
            trials.add(new TrialRow(l(f, 0), s(f, 1), l(f, 2), nullableLong(f, 3), "1".equals(f.get(5))));
        }
        appUsers = new ArrayList<>();
        for (List<String> f : rows(sql, "app_users")) {
            appUsers.add(new UserRow(l(f, 0), s(f, 1), s(f, 2), s(f, 4)));
        }
        sessions = new ArrayList<>();
        for (List<String> f : rows(sql, "game_sessions")) {
            sessions.add(new SessionRow(l(f, 0), s(f, 1), l(f, 2), s(f, 3), l(f, 4)));
        }
        answers = new ArrayList<>();
        for (List<String> f : rows(sql, "player_answers")) {
            answers.add(new AnswerRow(l(f, 0), l(f, 1), l(f, 2), l(f, 3), l(f, 4),
                    "1".equals(f.get(5)), nullableLong(f, 6)));
        }
        ratings = new ArrayList<>();
        for (List<String> f : rows(sql, "ratings")) {
            ratings.add(new RatingRow(l(f, 0), l(f, 1), l(f, 2), nullableLong(f, 3),
                    (int) l(f, 4), nullableLong(f, 5)));
        }

        wordsById = new HashMap<>();
        for (Word word : words) {
            wordsById.put(word.id(), word);
        }
    }

    @Test
    void seedsSixtyEightWordsSixtyTrialAndEightPractice() {
        // 60 trial words (a/v/i) plus 8 practice words (p), each a single row.
        assertEquals(68, words.size());
        assertEquals(60, words.stream().filter(w -> !isPractice(w)).count());
        assertEquals(8, words.stream().filter(IdeophoneSeedIntegrityTests::isPractice).count());
        // words UNIQUE(language_id, romaji): romaji distinct (one language in v1).
        assertEquals(68, words.stream().map(Word::romaji).distinct().count());
    }

    @Test
    void everyWordHasThreePresentationsOnePerScriptedCondition() {
        assertEquals(204, presentations.size());
        Map<Long, Set<String>> conditionsByWord = new HashMap<>();
        for (Presentation presentation : presentations) {
            assertNotNull(wordsById.get(presentation.wordId()),
                    "presentation " + presentation.id() + " references unknown word " + presentation.wordId());
            assertTrue(CONDITIONS.contains(presentation.conditionName()),
                    "unexpected condition " + presentation.conditionName());
            conditionsByWord.computeIfAbsent(presentation.wordId(), key -> new HashSet<>())
                    .add(presentation.conditionName());
        }
        assertEquals(68, conditionsByWord.size());
        for (Map.Entry<Long, Set<String>> entry : conditionsByWord.entrySet()) {
            assertEquals(CONDITIONS, entry.getValue(),
                    "word " + entry.getKey() + " must have exactly the three scripted conditions");
        }
    }

    @Test
    void wordStimulusFileIsPerWordAudioMatchingRomajiModalityAndCanonicalScript() {
        for (Word word : words) {
            Matcher matcher = AUDIO_PATTERN.matcher(word.stimulusFile());
            assertTrue(matcher.matches(),
                    wordLabel(word) + " has unexpected stimulus_file " + word.stimulusFile());
            if ("p".equals(matcher.group(1))) {
                assertTrue("AUDITORY".equals(word.modality()) || "VISUAL".equals(word.modality()),
                        wordLabel(word) + " practice word has unexpected modality " + word.modality());
            } else {
                assertEquals(MODALITY_BY_LETTER.get(matcher.group(1)), word.modality(),
                        wordLabel(word) + " stimulus modality letter disagrees with modality column");
            }
            // words.canonical_script is the word-level 'H' | 'K'; the audio pos3
            // (h/k) must agree with it.
            assertEquals(word.canonicalScript().toLowerCase(), matcher.group(3),
                    wordLabel(word) + " audio pos3 disagrees with canonical_script");
            assertEquals(word.romaji(), matcher.group(4),
                    wordLabel(word) + " stimulus romaji disagrees with romaji column");
        }
    }

    @Test
    void wordCanonicalFormScriptFamilyMatchesCanonicalScript() {
        for (Word word : words) {
            char script = word.canonicalScript().charAt(0);
            assertTrue(isInScriptFamily(word.canonicalForm(), script),
                    wordLabel(word) + " canonical_form " + word.canonicalForm() + " is not " + scriptName(script));
        }
    }

    @Test
    void presentationDisplayFormScriptFamilyMatchesScriptCodePosFour() {
        for (Presentation presentation : presentations) {
            Word word = wordsById.get(presentation.wordId());
            char pos4 = presentation.scriptCode().charAt(1);
            if (pos4 == 'U' || pos4 == 'D') {
                assertEquals(word.canonicalForm(), presentation.displayForm(),
                        "audio-only display_form must equal canonical_form for presentation " + presentation.id());
            } else {
                assertTrue(isInScriptFamily(presentation.displayForm(), pos4),
                        "presentation " + presentation.id() + " display_form " + presentation.displayForm()
                                + " is not " + scriptName(pos4));
            }
            // Presentation pos3 must match the word's canonical script.
            assertEquals(word.canonicalScript().charAt(0), presentation.scriptCode().charAt(0),
                    "presentation " + presentation.id() + " pos3 disagrees with the word's canonical script");
        }
    }

    @Test
    void pairingsBackfillIsThirtyThesisPairsPlusFourPractice() {
        assertEquals(34, pairings.size());
        assertEquals(34, pairings.stream().map(PairingRow::pairCode).distinct().count());
        long withThesisAccuracy = pairings.stream().filter(p -> !"NULL".equals(p.thesisAccuracy())).count();
        assertEquals(30, withThesisAccuracy, "the 30 thesis trial pairs carry an exact per-pair accuracy");
        for (PairingRow pairing : pairings) {
            assertTrue(pairing.core(), pairing.pairCode() + " must be is_core");
            assertEquals("THESIS", pairing.source(), pairing.pairCode() + " must be source THESIS");
            boolean practice = pairing.pairCode().startsWith("p");
            assertEquals(practice, "NULL".equals(pairing.thesisAccuracy()),
                    pairing.pairCode() + " thesis_accuracy presence must match trial/practice status");
        }
    }

    @Test
    void trialsCollapseToThirtyFourWithFourPractice() {
        assertEquals(34, trials.size());
        assertEquals(30, trials.stream().filter(t -> !t.practice()).count());
        assertEquals(4, trials.stream().filter(TrialRow::practice).count());
        for (TrialRow trial : trials) {
            assertEquals("CHOOSING", trial.roundType(), "M2 seeds CHOOSING trials only");
        }
    }

    // M2 invariant: every trial maps to a pairing whose two real word members
    // include the trial's correct word, and word_a id < word_b id (the
    // shuffle-order invariant that "pair second = higher word id" = word_b).
    @Test
    void everyTrialMapsToAPairingWhoseMembersIncludeItsCorrectWord() {
        Map<Long, PairingRow> pairingsById = new HashMap<>();
        for (PairingRow pairing : pairings) {
            pairingsById.put(pairing.id(), pairing);
        }
        for (TrialRow trial : trials) {
            PairingRow pairing = pairingsById.get(trial.pairingId());
            assertNotNull(pairing, "trial " + trial.id() + " references unknown pairing " + trial.pairingId());
            assertNotNull(wordsById.get(pairing.wordAId()), "pairing " + pairing.pairCode() + " word_a missing");
            assertNotNull(wordsById.get(pairing.wordBId()), "pairing " + pairing.pairCode() + " word_b missing");
            assertTrue(pairing.wordAId() < pairing.wordBId(),
                    "pairing " + pairing.pairCode() + " word_a id must be < word_b id (shuffle-order invariant)");
            assertNotNull(trial.correctWordId(), "trial " + trial.id() + " must have a correct word");
            assertTrue(trial.correctWordId() == pairing.wordAId() || trial.correctWordId() == pairing.wordBId(),
                    "trial " + trial.id() + " correct word must be a member of its pairing");
            // Same-modality contrastive members (invariant 4).
            assertEquals(wordsById.get(pairing.wordAId()).modality(), wordsById.get(pairing.wordBId()).modality(),
                    "pairing " + pairing.pairCode() + " members must share modality");
        }
    }

    @Test
    void glossTypoIsFixed() {
        for (Word word : words) {
            assertFalse(word.gloss().contains("feeling fo relief"),
                    wordLabel(word) + " still contains the gloss typo");
        }
    }

    // --- NIL-54 thesis tidy-data ingestion -----------------------------------

    @Test
    void thesisCohortIsThirtySixReservedUsersAlongsideTheAdmin() {
        assertEquals(37, appUsers.size(), "admin (id 1) plus the 36 thesis participants");
        assertTrue(appUsers.stream().anyMatch(u -> u.username().equals("arena_admin")), "admin must remain");
        List<UserRow> thesis = appUsers.stream().filter(u -> u.username().startsWith("thesis_p")).toList();
        assertEquals(36, thesis.size());
        Set<String> expected = new HashSet<>();
        for (int n = 1; n <= 36; n++) {
            expected.add(String.format("thesis_p%02d", n));
        }
        assertEquals(expected, thesis.stream().map(UserRow::username).collect(Collectors.toSet()),
                "usernames must be exactly thesis_p01..thesis_p36 (the reserved Rider A prefix)");
        for (UserRow user : thesis) {
            assertEquals("ROLE_USER", user.role(), user.username() + " must be ROLE_USER");
            assertEquals(user.username() + "@thesis.invalid", user.email());
        }
    }

    @Test
    void thesisSessionsAreThirtySixIncompleteWithTheConditionSplit() {
        // completed_at must be absent from the INSERT so it takes the DDL default
        // NULL: thesis sessions feed divergence but never the leaderboard.
        assertFalse(insertColumns(seedSql, "game_sessions").contains("completed_at"),
                "thesis sessions must take the completed_at DDL default (NULL)");
        assertEquals(36, sessions.size());
        assertEquals(36, sessions.stream().map(SessionRow::sessionUuid).distinct().count(),
                "session_uuid must be unique");
        Map<String, Long> byCondition = new HashMap<>();
        for (SessionRow session : sessions) {
            assertTrue(CONDITIONS.contains(session.conditionName()),
                    "unexpected condition " + session.conditionName());
            assertTrue(session.userId() >= 2 && session.userId() <= 37,
                    "thesis session must belong to a thesis user (ids 2..37)");
            byCondition.merge(session.conditionName(), 1L, Long::sum);
        }
        assertEquals(11L, byCondition.get("CONDITION_1_SOKUON"));
        assertEquals(13L, byCondition.get("CONDITION_2_SOKUON"));
        assertEquals(12L, byCondition.get("CONDITION_3_SOKUON"));
    }

    // The load-bearing reconciliation: the emitted answers, judged against each
    // trial's fixed thesis target (correct_word_id), reproduce the vendored
    // per-modality Choosing accuracy exactly -- AUDITORY 247/360 = 68.6%,
    // VISUAL 231/360 = 64.2%, INTEROCEPTIVE 215/360 = 59.7% (overall 693/1080).
    @Test
    void thesisAnswersReconstructTheThesisPerModalityAccuracy() {
        assertEquals(1080, answers.size());

        Set<String> sessionTrialKeys = new HashSet<>();
        Map<Long, Set<Long>> trialsBySession = new HashMap<>();
        for (AnswerRow answer : answers) {
            assertTrue(sessionTrialKeys.add(answer.sessionId() + ":" + answer.trialId()),
                    "UNIQUE(session_id, trial_id) violated at answer " + answer.id());
            trialsBySession.computeIfAbsent(answer.sessionId(), key -> new HashSet<>()).add(answer.trialId());
        }
        assertEquals(36, trialsBySession.size());
        trialsBySession.values().forEach(set -> assertEquals(30, set.size(),
                "each thesis session answers all 30 core trials"));

        Map<Long, TrialRow> trialById = new HashMap<>();
        for (TrialRow trial : trials) {
            trialById.put(trial.id(), trial);
        }

        Map<String, long[]> perModality = new HashMap<>();   // modality -> [total, correct]
        long totalCorrect = 0;
        for (AnswerRow answer : answers) {
            TrialRow trial = trialById.get(answer.trialId());
            assertNotNull(trial, "answer " + answer.id() + " references unknown trial " + answer.trialId());
            assertEquals(trial.correctWordId(), Long.valueOf(answer.targetWordId()),
                    "answer target_word_id must be the trial's fixed thesis target (correct_word_id)");
            String modality = wordsById.get(answer.targetWordId()).modality();
            long[] counts = perModality.computeIfAbsent(modality, key -> new long[2]);
            counts[0]++;
            if (answer.correct()) {
                counts[1]++;
                totalCorrect++;
            }
        }
        assertArrayEquals(new long[] {360, 247}, perModality.get("AUDITORY"), "auditory 247/360 = 68.6%");
        assertArrayEquals(new long[] {360, 231}, perModality.get("VISUAL"), "visual 231/360 = 64.2%");
        assertArrayEquals(new long[] {360, 215}, perModality.get("INTEROCEPTIVE"), "interoceptive 215/360 = 59.7%");
        assertEquals(693, totalCorrect, "overall 693/1080 = 64.17%");
    }

    @Test
    void thesisRatingsAreWellFormedAndUniquePerUserWord() {
        assertEquals(1080, ratings.size());
        Set<String> userWordKeys = new HashSet<>();
        Map<Long, Set<Long>> wordsByUser = new HashMap<>();
        for (RatingRow rating : ratings) {
            assertTrue(rating.rating() >= 1 && rating.rating() <= 7, "rating must be in 1..7");
            assertTrue(userWordKeys.add(rating.userId() + ":" + rating.wordId()),
                    "UNIQUE(user_id, word_id) violated at rating " + rating.id());
            assertTrue(rating.userId() >= 2 && rating.userId() <= 37,
                    "thesis rating must belong to a thesis user (ids 2..37)");
            assertNotNull(rating.sessionId(), "thesis rating keeps its session_id for provenance");
            wordsByUser.computeIfAbsent(rating.userId(), key -> new HashSet<>()).add(rating.wordId());
        }
        assertEquals(36, wordsByUser.size());
        wordsByUser.values().forEach(set -> assertEquals(30, set.size(),
                "each participant rated all 30 target words once"));
    }

    // --- SQL parsing helpers -------------------------------------------------

    // The declared column list of an `INSERT INTO <table> (...)` block, so a test
    // can assert a column is deliberately omitted (taking its DDL default).
    private static List<String> insertColumns(String sql, String table) {
        String marker = "INSERT INTO " + table + " (";
        int start = sql.indexOf(marker) + marker.length();
        int end = sql.indexOf(")", start);
        List<String> columns = new ArrayList<>();
        for (String column : sql.substring(start, end).split(",")) {
            columns.add(column.trim());
        }
        return columns;
    }

    // Extracts the value rows of an `INSERT INTO <table> (...) VALUES ...;`
    // block, each row split into fields (strings unquoted, bare tokens verbatim).
    private static List<List<String>> rows(String sql, String table) {
        int insertStart = sql.indexOf("INSERT INTO " + table + " (");
        int valuesStart = sql.indexOf("VALUES", insertStart) + "VALUES".length();
        int insertEnd = sql.indexOf(";", valuesStart);
        String block = sql.substring(valuesStart, insertEnd);

        List<List<String>> parsed = new ArrayList<>();
        for (String line : block.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("(")) {
                parsed.add(parseRow(trimmed));
            }
        }
        return parsed;
    }

    // Splits one `(a, 'b', NULL, 0.94)` tuple into fields, honoring single-quoted
    // strings (with '' un-escaping) and ignoring the trailing comma/paren.
    private static List<String> parseRow(String row) {
        String inner = row.substring(1, row.lastIndexOf(')'));
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < inner.length(); i++) {
            char ch = inner.charAt(i);
            if (inString) {
                if (ch == '\'') {
                    if (i + 1 < inner.length() && inner.charAt(i + 1) == '\'') {
                        current.append('\'');
                        i++;
                    } else {
                        inString = false;
                    }
                } else {
                    current.append(ch);
                }
            } else if (ch == '\'') {
                inString = true;
            } else if (ch == ',') {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        fields.add(current.toString().trim());
        return fields;
    }

    private static long l(List<String> fields, int index) {
        return Long.parseLong(fields.get(index));
    }

    private static Long nullableLong(List<String> fields, int index) {
        String value = fields.get(index);
        return "NULL".equals(value) ? null : Long.parseLong(value);
    }

    private static String s(List<String> fields, int index) {
        return fields.get(index);
    }

    private static boolean isPractice(Word word) {
        return word.stimulusFile().startsWith("audio/p");
    }

    private static boolean isInScriptFamily(String text, char scriptCode) {
        boolean hiragana = scriptCode == 'H';
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == 'ー') {
                continue; // long vowel mark is valid in either script
            }
            boolean inHiraganaBlock = ch >= 'ぁ' && ch <= 'ゖ';
            boolean inKatakanaBlock = ch >= 'ァ' && ch <= 'ヺ';
            if (hiragana ? !inHiraganaBlock : !inKatakanaBlock) {
                return false;
            }
        }
        return true;
    }

    private static String scriptName(char scriptCode) {
        return scriptCode == 'H' ? "hiragana" : "katakana";
    }

    private static String wordLabel(Word word) {
        return "word id " + word.id() + " (" + word.romaji() + ", " + word.canonicalScript() + ")";
    }
}
