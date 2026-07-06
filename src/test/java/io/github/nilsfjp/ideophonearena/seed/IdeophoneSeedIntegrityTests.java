package io.github.nilsfjp.ideophonearena.seed;

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

    private static List<Word> words;
    private static List<Presentation> presentations;
    private static List<PairingRow> pairings;
    private static List<TrialRow> trials;
    private static Map<Long, Word> wordsById;

    @BeforeAll
    static void parseSeed() throws IOException {
        String sql = Files.readString(SEED_PATH, StandardCharsets.UTF_8);

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

    // --- SQL parsing helpers -------------------------------------------------

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
