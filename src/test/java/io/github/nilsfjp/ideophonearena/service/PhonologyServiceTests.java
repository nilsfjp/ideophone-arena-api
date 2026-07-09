package io.github.nilsfjp.ideophonearena.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.nilsfjp.ideophonearena.exception.UnparseableInputException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

// The Java half of the ADR-8.2 dual-implementation contract. The Python half
// (scripts/generate_seed_sql.py) emits docs/research/phonology-golden.json via
// scripts/generate_phonology_golden.py; this asserts byte-for-byte agreement against the
// whole committed file, so either side drifting breaks loudly.
class PhonologyServiceTests {

    private static final Path GOLDEN_PATH = Path.of("docs/research/phonology-golden.json");

    private final PhonologyService phonology = new PhonologyService();
    private final PhonologyProfile japanese = PhonologyProfile.JAPANESE;

    private static JsonNode golden;

    @BeforeAll
    static void readGolden() throws IOException {
        ObjectMapper objectMapper = JsonMapper.builder().build();
        golden = objectMapper.readTree(Files.readString(GOLDEN_PATH, StandardCharsets.UTF_8));
    }

    private PhonologyFeatures features(String canonicalRomaji) {
        return phonology.features(canonicalRomaji, japanese);
    }

    private List<String> morae(String canonicalRomaji) {
        return phonology.morae(canonicalRomaji, japanese);
    }

    private String normalize(String raw) {
        return phonology.normalizeInput(raw, japanese);
    }

    private int score(String playerInput, String targetRomaji) {
        return phonology.score(features(normalize(playerInput)), features(targetRomaji));
    }

    // ----- ADR-8.2 golden parity: the entire committed file -----

    @Test
    void everyGoldenWordMatchesTheJavaFeatureExtractor() {
        JsonNode words = golden.get("words");
        assertEquals(102, words.size(), "golden word count");

        for (JsonNode word : words) {
            String romaji = word.get("romaji").asString();

            List<String> expectedMorae = new ArrayList<>();
            word.get("morae").forEach(mora -> expectedMorae.add(mora.asString()));
            assertEquals(expectedMorae, morae(romaji), "morae for " + romaji);

            PhonologyFeatures actual = features(romaji);
            assertEquals(word.get("mora_count").asInt(), actual.moraCount(), "mora_count for " + romaji);
            assertEquals(word.get("redup").asBoolean(), actual.redup(), "redup for " + romaji);
            assertEquals(word.get("sokuon").asBoolean(), actual.sokuon(), "sokuon for " + romaji);
            assertEquals(word.get("final_n").asBoolean(), actual.finalN(), "final_n for " + romaji);
            assertEquals(word.get("ri_suffix").asBoolean(), actual.riSuffix(), "ri_suffix for " + romaji);
            assertEquals(word.get("voiced_onset").asBoolean(), actual.voicedOnset(), "voiced_onset for " + romaji);
            assertEquals(word.get("heavy_vowels").asInt(), actual.heavyVowelCount(), "heavy_vowels for " + romaji);
            assertEquals(word.get("light_vowels").asInt(), actual.lightVowelCount(), "light_vowels for " + romaji);
        }
    }

    @Test
    void everyGoldenExpansionPairMatchesTheJavaDistance() {
        JsonNode pairs = golden.get("expansion_pairs");
        assertEquals(21, pairs.size(), "golden expansion pair count");

        for (JsonNode pair : pairs) {
            String wordA = pair.get("word_a").asString();
            String wordB = pair.get("word_b").asString();
            String expected = pair.get("foil_distance").asString();
            String actual = phonology.distance(features(wordA), features(wordB)).toPlainString();
            assertEquals(expected, actual, "foil_distance for " + pair.get("pair_code").asString());
        }
    }

    @Test
    void everySeededRomajiSegmentsWithoutError() {
        for (JsonNode word : golden.get("words")) {
            String romaji = word.get("romaji").asString();
            assertFalse(morae(romaji).isEmpty(), "morae for " + romaji);
        }
    }

    // Why normalizeInput() and morae()/features() must be separate entry points: the player
    // path re-reads a moraic nasal before y as a palatalized token, so feeding seeded romaji
    // back through it silently changes the mora count. Exactly two seeded words are affected.
    @Test
    void inventoryRomajiMustBypassThePlayerNormalizer() {
        List<String> corrupted = new ArrayList<>();
        for (JsonNode word : golden.get("words")) {
            String romaji = word.get("romaji").asString();
            if (!romaji.equals(normalize(romaji))) {
                corrupted.add(romaji);
            }
        }
        assertEquals(List.of("doNyori", "boNyari"), corrupted, "seeded romaji the player path rewrites");

        assertEquals(List.of("do", "N", "yo", "ri"), morae("doNyori"));
        assertEquals(4, features("doNyori").moraCount());
        assertEquals(List.of("do", "nyo", "ri"), morae(normalize("doNyori")));
        assertEquals(3, features(normalize("doNyori")).moraCount());
    }

    // ----- SPEC section 10.1 golden cases -----

    @Test
    void identicalFormsScoreOneHundred() {
        assertEquals(100, score("gosogoso", "gosogoso"));
    }

    @Test
    void trailingQBecomesAWordFinalSokuon() {
        assertEquals("paQ", normalize("paq"));
        assertEquals(List.of("pa", "Q"), morae("paQ"));
        assertTrue(features("paQ").sokuon());
    }

    @Test
    void hepburnFoldsToKunrei() {
        assertEquals("sittori", normalize("shittori"));
        assertEquals(List.of("si", "Q", "to", "ri"), morae("sittori"));
    }

    @Test
    void doubledVowelsCountAsTwoMorae() {
        assertEquals(List.of("zya", "a", "zya", "a"), morae("zyaazyaa"));
        assertEquals(4, features("zyaazyaa").moraCount());
    }

    @Test
    void unsegmentableInputIsRejected() {
        assertThrows(UnparseableInputException.class, () -> normalize("ngrk"));
    }

    // The worked example on the adjudicated Word Mint mockup (SPEC-view-designs V10-V14).
    // Raw similarity is exactly 77.500, so this case also pins the rounding mode as observable.
    @Test
    void theAdjudicatedWorkedExampleScoresSeventyEight() {
        assertEquals(78, score("pikapika", "dokidoki"));
    }

    // ----- the seven silent-divergence traps -----

    @Test
    void voicedOnsetReadsTheFirstMoraNotTheFirstLetter() {
        // A leading geminate or moraic nasal owns the first mora, so the onset is Q / N.
        assertEquals(List.of("Q", "ga"), morae(normalize("gga")));
        assertFalse(features(normalize("gga")).voicedOnset());
        assertEquals(List.of("N", "go"), morae(normalize("ngo")));
        assertFalse(features(normalize("ngo")).voicedOnset());
        // ...while a palatalized voiced onset still counts.
        assertTrue(features("gyuQ").voicedOnset());
        assertTrue(features("zyaazyaa").voicedOnset());
    }

    @Test
    void riSuffixIsSuppressedByReduplication() {
        // Seven seeded words end in -ri and reduplicate; the golden pins ri_suffix false.
        for (String romaji : List.of("ziriziri", "poripori", "kurikuri", "garigari",
                "girigiri", "boribori", "sororisorori")) {
            PhonologyFeatures actual = features(romaji);
            assertTrue(actual.redup(), "redup for " + romaji);
            assertFalse(actual.riSuffix(), "ri_suffix for " + romaji);
        }
        // ...and a non-reduplicated -ri word does carry it.
        assertTrue(features("uttori").riSuffix());
    }

    @Test
    void geminationRunsInsideSegmentationAfterTheNasalFold() {
        // "nn" is the discriminating case: the nasal fold wins, so this is N.N, not Q.N.
        assertEquals(List.of("N", "N"), morae(normalize("nn")));
        assertFalse(features(normalize("nn")).sokuon());
        // A doubled consonant before a palatalized token still yields Q + CyV.
        assertEquals(List.of("Q", "sya"), morae(normalize("ssha")));
        assertEquals(List.of("ma", "Q", "tya"), morae(normalize("matcha")));
        assertEquals(List.of("u", "Q", "to", "ri"), morae("uttori"));
        // Long vowels never geminate.
        assertEquals(List.of("a", "a"), morae("aa"));
    }

    @Test
    void reduplicationNeedsFourMoraeAndEvenHalves() {
        assertFalse(features("kaka").redup(), "2 morae is X.X with X = 1 mora");
        assertTrue(features("gaNgaN").redup());
        assertTrue(features("ruNruN").redup());
        assertFalse(features("gosogosogoso").redup(), "6 morae, halves differ");
    }

    @Test
    void vowelWeightIgnoresTheLowVowelAndTheMarkerMorae() {
        // katiQ: ka (a, neither) / ti (i, light) / Q (skipped) -> 0 heavy, 1 light.
        PhonologyFeatures katiq = features("katiQ");
        assertEquals(0, katiq.heavyVowelCount());
        assertEquals(1, katiq.lightVowelCount());
        // zyaazyaa is all 'a': the denominator is zero, so the ratio is the neutral 0.5.
        PhonologyFeatures zyaazyaa = features("zyaazyaa");
        assertEquals(0, zyaazyaa.heavyVowelCount());
        assertEquals(0, zyaazyaa.lightVowelCount());
        assertEquals(0, new java.math.BigDecimal("0.5").compareTo(zyaazyaa.heavyVowelRatio()));
    }

    @Test
    void inputIsTrimmedAndLowercasedBeforeTheGate() {
        assertEquals("dokidoki", normalize("  DokiDoki  "));
        assertThrows(UnparseableInputException.class, () -> normalize("a"));
        assertThrows(UnparseableInputException.class, () -> normalize("pika pika"));
        assertThrows(UnparseableInputException.class, () -> normalize("pika3"));
        assertThrows(UnparseableInputException.class, () -> normalize("aaaaaaaaaaaaaaaaaaaaaaaaa"));
    }

    @Test
    void danglingConsonantRunsFailToSegment() {
        for (String raw : List.of("ttty", "kk", "yy", "ww", "qq")) {
            assertThrows(UnparseableInputException.class, () -> normalize(raw), raw);
        }
    }

    // The h/f class the Python side caught: Hepburn fu folds to Kunrei hu.
    @Test
    void theFuHuFoldIsLoadBearing() {
        assertEquals("husahusa", normalize("fusafusa"));
        assertEquals("huNwari", normalize("funwari"));
        assertEquals(List.of("hu", "sa", "hu", "sa"), morae("husahusa"));
    }

    // A player typing plain Hepburn lands exactly on the seeded canonical romaji.
    @Test
    void normalizingHepburnReproducesTheSeededCanonicalForm() {
        assertEquals("gaNgaN", normalize("gangan"));
        assertEquals("turuturu", normalize("tsurutsuru"));
        assertEquals("zyaazyaa", normalize("jaajaa"));
        assertEquals("syoboN", normalize("shobon"));
        assertEquals("geNnari", normalize("gennari"));
    }

    // Legal 24-char input can fold to 36 chars, which is why normalized_form is VARCHAR(40).
    @Test
    void theLongestLegalInputStillFitsTheColumn() {
        String worstCase = "ja".repeat(12);
        assertEquals(24, worstCase.length());
        String normalized = normalize(worstCase);
        assertEquals("zya".repeat(12), normalized);
        assertEquals(36, normalized.length());
        assertEquals(12, morae(normalized).size());
    }
}
