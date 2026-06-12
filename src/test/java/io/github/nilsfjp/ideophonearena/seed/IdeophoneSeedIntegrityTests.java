package io.github.nilsfjp.ideophonearena.seed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Guards the experiment invariants of the ideophone seed: the canonical_script
 * code (pos3+pos4 of the original stimulus filename prefix) is ground truth for
 * which kana script canonical_form and display_form must use, and every row of
 * a word must share that word's single per-word audio stimulus.
 */
class IdeophoneSeedIntegrityTests {

    private static final Path SEED_PATH =
            Path.of("src/main/resources/db/init/ideophone_arena.sql");

    private static final Pattern ROW_PATTERN = Pattern.compile(
            "\\((\\d+), '([^']+)', '([^']+)', '([^']+)', '([^']+)', '([^']+)', '([^']+)', '([^']+)', '([^']+)'\\)");

    private static final Pattern AUDIO_PATTERN = Pattern.compile(
            "audio/([avip])(\\d)([hk])-([A-Za-z]+)\\.m4a");

    private static final Map<String, String> MODALITY_BY_LETTER = Map.of(
            "a", "AUDITORY",
            "v", "VISUAL",
            "i", "INTEROCEPTIVE");

    private static final Pattern ROUND_ROW_PATTERN = Pattern.compile(
            "\\((\\d+), '[^']*', (\\d+), (\\d+), (\\d+), '[^']+', \\d+, (\\d+)\\)");

    private record SeedRow(long id, String kana, String displayForm, String canonicalForm,
            String romaji, String gloss, String canonicalScript, String stimulusFile, String modality) {
    }

    private record RoundRow(long id, long leftId, long rightId, long correctId, boolean practice) {
    }

    private static List<SeedRow> rows;
    private static List<RoundRow> roundRows;

    @BeforeAll
    static void parseSeed() throws IOException {
        String sql = Files.readString(SEED_PATH, StandardCharsets.UTF_8);
        int insertStart = sql.indexOf("INSERT INTO ideophones");
        int insertEnd = sql.indexOf(';', insertStart);
        String block = sql.substring(insertStart, insertEnd);

        rows = new ArrayList<>();
        Matcher matcher = ROW_PATTERN.matcher(block);
        while (matcher.find()) {
            rows.add(new SeedRow(
                    Long.parseLong(matcher.group(1)),
                    matcher.group(2),
                    matcher.group(3),
                    matcher.group(4),
                    matcher.group(5),
                    matcher.group(6),
                    matcher.group(7),
                    matcher.group(8),
                    matcher.group(9)));
        }

        int roundInsertStart = sql.indexOf("INSERT INTO arena_rounds");
        int roundInsertEnd = sql.indexOf(';', roundInsertStart);
        String roundBlock = sql.substring(roundInsertStart, roundInsertEnd);

        roundRows = new ArrayList<>();
        Matcher roundMatcher = ROUND_ROW_PATTERN.matcher(roundBlock);
        while (roundMatcher.find()) {
            roundRows.add(new RoundRow(
                    Long.parseLong(roundMatcher.group(1)),
                    Long.parseLong(roundMatcher.group(2)),
                    Long.parseLong(roundMatcher.group(3)),
                    Long.parseLong(roundMatcher.group(4)),
                    !"0".equals(roundMatcher.group(5))));
        }
    }

    @Test
    void seedsAllTwoHundredFourIdeophoneRows() {
        // 60 trial words plus 8 practice words, each in three condition variants.
        assertEquals(204, rows.size());
        assertEquals(24, rows.stream().filter(IdeophoneSeedIntegrityTests::isPracticeRow).count());
    }

    @Test
    void canonicalScriptCodesAreWellFormed() {
        for (SeedRow row : rows) {
            assertTrue(row.canonicalScript().matches("[HK][HKUD]"),
                    rowLabel(row) + " has malformed canonical_script " + row.canonicalScript());
        }
    }

    @Test
    void stimulusFileIsPerWordAudioMatchingRomajiModalityAndCanonicalScript() {
        for (SeedRow row : rows) {
            Matcher matcher = AUDIO_PATTERN.matcher(row.stimulusFile());
            assertTrue(matcher.matches(),
                    rowLabel(row) + " has unexpected stimulus_file " + row.stimulusFile());
            if ("p".equals(matcher.group(1))) {
                // Practice prefix carries no modality letter; the thesis only
                // has auditory and visual practice pairs.
                assertTrue("AUDITORY".equals(row.modality()) || "VISUAL".equals(row.modality()),
                        rowLabel(row) + " practice row has unexpected modality " + row.modality());
            } else {
                assertEquals(MODALITY_BY_LETTER.get(matcher.group(1)), row.modality(),
                        rowLabel(row) + " stimulus modality letter disagrees with modality column");
            }
            assertEquals(row.canonicalScript().substring(0, 1).toLowerCase(), matcher.group(3),
                    rowLabel(row) + " stimulus pos3 disagrees with canonical_script");
            assertEquals(row.romaji(), matcher.group(4),
                    rowLabel(row) + " stimulus romaji disagrees with romaji column");
        }
    }

    @Test
    void canonicalFormScriptFamilyMatchesPosThree() {
        for (SeedRow row : rows) {
            char pos3 = row.canonicalScript().charAt(0);
            assertTrue(isInScriptFamily(row.canonicalForm(), pos3),
                    rowLabel(row) + " canonical_form " + row.canonicalForm()
                            + " is not " + scriptName(pos3));
        }
    }

    @Test
    void displayFormScriptFamilyMatchesPosFour() {
        for (SeedRow row : rows) {
            char pos4 = row.canonicalScript().charAt(1);
            if (pos4 == 'U' || pos4 == 'D') {
                assertEquals(row.canonicalForm(), row.displayForm(),
                        rowLabel(row) + " audio-only display_form must equal canonical_form");
            } else {
                assertTrue(isInScriptFamily(row.displayForm(), pos4),
                        rowLabel(row) + " display_form " + row.displayForm()
                                + " is not " + scriptName(pos4));
            }
        }
    }

    @Test
    void everyWordSharesOneAudioFileAcrossItsThreeConditionRows() {
        Map<String, List<SeedRow>> byAudio = new java.util.HashMap<>();
        for (SeedRow row : rows) {
            byAudio.computeIfAbsent(row.stimulusFile(), key -> new ArrayList<>()).add(row);
        }
        assertEquals(68, byAudio.size());
        for (Map.Entry<String, List<SeedRow>> entry : byAudio.entrySet()) {
            assertEquals(3, entry.getValue().size(),
                    entry.getKey() + " is not shared by exactly three condition rows");
        }
    }

    @Test
    void practiceRoundsAreFlaggedAndReferenceOnlyPracticeIdeophones() {
        assertEquals(102, roundRows.size());
        assertEquals(12, roundRows.stream().filter(RoundRow::practice).count());

        Map<Long, SeedRow> rowsById = new java.util.HashMap<>();
        for (SeedRow row : rows) {
            rowsById.put(row.id(), row);
        }
        for (RoundRow round : roundRows) {
            for (long ideophoneId : new long[] {round.leftId(), round.rightId(), round.correctId()}) {
                SeedRow row = rowsById.get(ideophoneId);
                assertTrue(row != null, "round " + round.id() + " references unknown ideophone " + ideophoneId);
                assertEquals(round.practice(), isPracticeRow(row),
                        "round " + round.id() + " practice flag disagrees with ideophone "
                                + rowLabel(row));
            }
        }
    }

    @Test
    void glossTypoIsFixed() {
        for (SeedRow row : rows) {
            assertFalse(row.gloss().contains("feeling fo relief"),
                    rowLabel(row) + " still contains the gloss typo");
        }
    }

    private static boolean isPracticeRow(SeedRow row) {
        return row.stimulusFile().startsWith("audio/p");
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

    private static String rowLabel(SeedRow row) {
        return "ideophone id " + row.id() + " (" + row.romaji() + ", " + row.canonicalScript() + ")";
    }
}
