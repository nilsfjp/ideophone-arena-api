package io.github.nilsfjp.ideophonearena.service;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

// Per-language phonology rules (ADR-8.1 / NIL-84 X8). Every PhonologyService method
// takes one of these, so a second language is a new constant rather than a code change.
// JAPANESE is the only v1 profile; its tables are the byte-for-byte counterparts of
// KANA_TOKENS / HEPBURN_FOLDS in scripts/generate_seed_sql.py (ADR-8.2 dual implementation,
// asserted against docs/research/phonology-golden.json).
public record PhonologyProfile(
        String languageIsoCode,
        Set<String> moraTokens,
        List<Fold> orthographyFolds,
        Pattern inputPattern,
        Pattern moraicNasalPattern,
        char sokuonInputLetter,
        String moraicNasalMarker,
        String sokuonMarker,
        Set<Character> geminateExcludedLetters,
        Set<Character> heavyVowels,
        Set<Character> lightVowels,
        Set<Character> voicedOnsetLetters) {

    // One orthography-normalizing rewrite, applied as an ordered replace-all.
    public record Fold(String source, String target) {
    }

    private static final int MAX_TOKEN_WIDTH = 3;

    // The 103 KANA_TOKENS keys. No bare "n" (it folds to N or joins a CV/CyV token),
    // no dya/dyu/dyo, no wi/we/ye -- an input needing them fails to segment, which is
    // the intended 400.
    private static final Set<String> JAPANESE_MORA_TOKENS = Set.of(
            "a", "i", "u", "e", "o",
            "ka", "ki", "ku", "ke", "ko",
            "sa", "si", "su", "se", "so",
            "ta", "ti", "tu", "te", "to",
            "na", "ni", "nu", "ne", "no",
            "ha", "hi", "hu", "he", "ho",
            "ma", "mi", "mu", "me", "mo",
            "ya", "yu", "yo",
            "ra", "ri", "ru", "re", "ro",
            "wa", "wo",
            "ga", "gi", "gu", "ge", "go",
            "za", "zi", "zu", "ze", "zo",
            "da", "di", "du", "de", "do",
            "ba", "bi", "bu", "be", "bo",
            "pa", "pi", "pu", "pe", "po",
            "kya", "kyu", "kyo",
            "sya", "syu", "syo",
            "tya", "tyu", "tyo",
            "nya", "nyu", "nyo",
            "hya", "hyu", "hyo",
            "mya", "myu", "myo",
            "rya", "ryu", "ryo",
            "gya", "gyu", "gyo",
            "zya", "zyu", "zyo",
            "bya", "byu", "byo",
            "pya", "pyu", "pyo");

    // Hepburn -> Kunrei/Nihon-shiki, in the generator's exact order (3-char before the
    // shorter same-prefix form). Applied as sequential replace-all, mirroring Python's
    // str.replace chain: no fold source is a substring of any fold target, so no rewrite
    // cascades, but the order is kept anyway because it is the parity target.
    private static final List<Fold> JAPANESE_FOLDS = List.of(
            new Fold("sha", "sya"), new Fold("shu", "syu"), new Fold("sho", "syo"), new Fold("shi", "si"),
            new Fold("cha", "tya"), new Fold("chu", "tyu"), new Fold("cho", "tyo"), new Fold("chi", "ti"),
            new Fold("tsu", "tu"),
            new Fold("ja", "zya"), new Fold("ju", "zyu"), new Fold("jo", "zyo"), new Fold("ji", "zi"),
            new Fold("fu", "hu"));

    public static final PhonologyProfile JAPANESE = new PhonologyProfile(
            "jpn",
            JAPANESE_MORA_TOKENS,
            JAPANESE_FOLDS,
            // Player gate. Inventory romaji carries N/Q and must never pass through here.
            Pattern.compile("[a-z]{2,24}"),
            // Syllabic/final n: n before y or a vowel stays lowercase and joins its token.
            Pattern.compile("n(?![aiueoy])"),
            'q',
            "N",
            "Q",
            // A doubled letter becomes a sokuon unless it is a vowel or n (long vowels are
            // two morae; nn is two moraic nasals, not sokuon + nasal).
            Set.of('a', 'e', 'i', 'o', 'u', 'n'),
            Set.of('o', 'u'),
            Set.of('i', 'e'),
            Set.of('g', 'z', 'd', 'b'));

    public int maxTokenWidth() {
        return MAX_TOKEN_WIDTH;
    }
}
