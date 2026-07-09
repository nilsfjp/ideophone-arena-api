package io.github.nilsfjp.ideophonearena.service;

import io.github.nilsfjp.ideophonearena.exception.UnparseableInputException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

// The feature/normalization engine of SPEC-free-form-entry section 5. Pure functions, no
// repository access; every method takes a PhonologyProfile (ADR-8.1 seam). Three consumers
// are planned: production scoring (here), word_features emission, and the NIL-58 distance
// covariate -- which is why features() and distance() are public and independent of the web layer.
//
// Scope fence (binding, ADR-8.1): the similarity formula is a production-scoring instrument --
// player invention vs attested target. It is never a difficulty dial between real words.
//
// Parity: this is the Java half of the ADR-8.2 dual implementation. The Python half lives in
// scripts/generate_seed_sql.py (mora_segments / feature_vector / foil_distance) and both sides
// are asserted against docs/research/phonology-golden.json.
@Service
public class PhonologyService {

    // Bumped whenever the weights or the feature set change, so stored scores stay
    // interpretable and raw_input can be re-scored under any past version.
    public static final short SCORER_VERSION = 1;

    // Mirrors the generator's decimal context (getcontext().prec = 50; Python's default
    // rounding is ROUND_HALF_EVEN). Python rounds after every arithmetic op, so the context
    // is passed to every op here rather than only to the divisions.
    private static final MathContext MC = new MathContext(50, RoundingMode.HALF_EVEN);

    private static final int DISTANCE_SCALE = 4;

    private static final BigDecimal W_REDUP = new BigDecimal("0.20");
    private static final BigDecimal W_SOKUON = new BigDecimal("0.15");
    private static final BigDecimal W_FINAL_N = new BigDecimal("0.10");
    private static final BigDecimal W_RI_SUFFIX = new BigDecimal("0.10");
    private static final BigDecimal W_VOICED_ONSET = new BigDecimal("0.15");
    private static final BigDecimal W_HEAVY_VOWEL = new BigDecimal("0.15");
    private static final BigDecimal W_MORA_COUNT = new BigDecimal("0.15");

    private static final BigDecimal NEUTRAL_HEAVY_RATIO = new BigDecimal("0.5");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    // The player entry point: raw keyboard input -> canonical romaji. Only this method
    // applies the a-z gate and the orthography folds. Inventory romaji already carries the
    // N/Q markers and must go straight to morae()/features().
    public String normalizeInput(String raw, PhonologyProfile profile) {
        if (raw == null) {
            throw new UnparseableInputException("Enter a word using roman letters");
        }
        String text = raw.trim().toLowerCase(Locale.ROOT);
        if (!profile.inputPattern().matcher(text).matches()) {
            throw new UnparseableInputException("Use 2 to 24 roman letters, like gorogoro or pika");
        }
        for (PhonologyProfile.Fold fold : profile.orthographyFolds()) {
            text = text.replace(fold.source(), fold.target());
        }
        text = profile.moraicNasalPattern().matcher(text).replaceAll(profile.moraicNasalMarker());
        // Only a trailing sokuon letter is a sokuon: the word-final geminate (paq -> paQ).
        if (!text.isEmpty() && text.charAt(text.length() - 1) == profile.sokuonInputLetter()) {
            text = text.substring(0, text.length() - 1) + profile.sokuonMarker();
        }
        try {
            morae(text, profile);
        } catch (IllegalArgumentException ex) {
            throw new UnparseableInputException("That did not read as speakable syllables");
        }
        return text;
    }

    // Segments canonical romaji into morae. Accepts the N/Q markers, so it is safe for both
    // seeded words.romaji and a normalizeInput() result. A doubled consonant becomes a Q mora,
    // which is why the geminate test runs before the token lookup -- and why it excludes
    // vowels (a long vowel is two morae) and n (nn is two moraic nasals, not Q + N).
    public List<String> morae(String canonicalRomaji, PhonologyProfile profile) {
        List<String> morae = new ArrayList<>();
        int index = 0;
        int length = canonicalRomaji.length();
        while (index < length) {
            String current = String.valueOf(canonicalRomaji.charAt(index));
            if (current.equals(profile.moraicNasalMarker()) || current.equals(profile.sokuonMarker())) {
                morae.add(current);
                index++;
                continue;
            }
            char letter = canonicalRomaji.charAt(index);
            if (index + 1 < length
                    && letter == canonicalRomaji.charAt(index + 1)
                    && !profile.geminateExcludedLetters().contains(letter)) {
                morae.add(profile.sokuonMarker());
                index++;
                continue;
            }
            boolean matched = false;
            for (int width = profile.maxTokenWidth(); width >= 1; width--) {
                if (index + width > length) {
                    continue;
                }
                String token = canonicalRomaji.substring(index, index + width);
                if (profile.moraTokens().contains(token)) {
                    morae.add(token);
                    index += width;
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                throw new IllegalArgumentException(
                        "Cannot segment romaji " + canonicalRomaji + " near " + canonicalRomaji.substring(index));
            }
        }
        return morae;
    }

    public PhonologyFeatures features(String canonicalRomaji, PhonologyProfile profile) {
        List<String> morae = morae(canonicalRomaji, profile);
        int moraCount = morae.size();

        int heavy = 0;
        int light = 0;
        for (String mora : morae) {
            if (mora.equals(profile.moraicNasalMarker()) || mora.equals(profile.sokuonMarker())) {
                continue;
            }
            char vowel = mora.charAt(mora.length() - 1);
            if (profile.heavyVowels().contains(vowel)) {
                heavy++;
            } else if (profile.lightVowels().contains(vowel)) {
                light++;
            }
            // 'a' is neither heavy nor light: it leaves the ratio untouched.
        }
        BigDecimal heavyVowelRatio = heavy + light == 0
                ? NEUTRAL_HEAVY_RATIO
                : new BigDecimal(heavy).divide(new BigDecimal(heavy + light), MC);

        // X.X with X at least 2 morae -- so a 4-mora minimum, and never an odd count.
        boolean redup = moraCount >= 4
                && moraCount % 2 == 0
                && morae.subList(0, moraCount / 2).equals(morae.subList(moraCount / 2, moraCount));

        String lastMora = morae.get(moraCount - 1);
        // The onset is the first mora's leading letter, not the input's: a leading geminate
        // or moraic nasal (gga -> Q.ga, ngo -> N.go) is an unvoiced onset.
        char onset = morae.get(0).charAt(0);

        return new PhonologyFeatures(
                moraCount,
                redup,
                morae.contains(profile.sokuonMarker()),
                lastMora.equals(profile.moraicNasalMarker()),
                lastMora.equals("ri") && !redup,
                profile.voicedOnsetLetters().contains(onset),
                heavy,
                light,
                heavyVowelRatio);
    }

    // Weighted featural agreement in [0, 1]. Weights sum to 1.00 and are v1-tunable --
    // never retro-edit without bumping SCORER_VERSION.
    public BigDecimal similarity(PhonologyFeatures player, PhonologyFeatures target) {
        BigDecimal heavyTerm = BigDecimal.ONE.subtract(
                player.heavyVowelRatio().subtract(target.heavyVowelRatio(), MC).abs(), MC);

        int moraDelta = Math.abs(player.moraCount() - target.moraCount());
        int moraMax = Math.max(player.moraCount(), target.moraCount());
        BigDecimal moraTerm = BigDecimal.ONE.subtract(
                new BigDecimal(moraDelta).divide(new BigDecimal(moraMax), MC), MC);

        return W_REDUP.multiply(agree(player.redup(), target.redup()), MC)
                .add(W_SOKUON.multiply(agree(player.sokuon(), target.sokuon()), MC), MC)
                .add(W_FINAL_N.multiply(agree(player.finalN(), target.finalN()), MC), MC)
                .add(W_RI_SUFFIX.multiply(agree(player.riSuffix(), target.riSuffix()), MC), MC)
                .add(W_VOICED_ONSET.multiply(agree(player.voicedOnset(), target.voicedOnset()), MC), MC)
                .add(W_HEAVY_VOWEL.multiply(heavyTerm, MC), MC)
                .add(W_MORA_COUNT.multiply(moraTerm, MC), MC);
    }

    // Symmetric featural distance in [0, 1], quantized to 4 places. This is the primitive
    // NIL-58 borrows and the value pairings.foil_distance stores; it is a recorded covariate,
    // never an ordering or difficulty input for core content (ADR-6).
    public BigDecimal distance(PhonologyFeatures a, PhonologyFeatures b) {
        return BigDecimal.ONE.subtract(similarity(a, b), MC)
                .setScale(DISTANCE_SCALE, RoundingMode.HALF_EVEN);
    }

    // The stored 0-100 score. HALF_EVEN because 100 x similarity lands on an exact .5 tie for
    // roughly a third of word pairs, and HALF_EVEN is the rounding the generator already uses
    // for foil_distance -- one rounding convention across the whole scorer. An exact-form
    // match scores exactly 100.
    public int score(PhonologyFeatures player, PhonologyFeatures target) {
        return similarity(player, target)
                .multiply(HUNDRED, MC)
                .setScale(0, RoundingMode.HALF_EVEN)
                .intValueExact();
    }

    private static BigDecimal agree(boolean left, boolean right) {
        return left == right ? BigDecimal.ONE : BigDecimal.ZERO;
    }
}
