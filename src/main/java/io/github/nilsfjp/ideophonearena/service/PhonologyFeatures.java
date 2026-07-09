package io.github.nilsfjp.ideophonearena.service;

import java.math.BigDecimal;

// The 7 whole-form features of SPEC-free-form-entry section 5, plus the raw heavy/light
// mora counts that docs/research/phonology-golden.json records (the ratio is derived from
// them, and the golden pins the counts).
public record PhonologyFeatures(
        int moraCount,
        boolean redup,
        boolean sokuon,
        boolean finalN,
        boolean riSuffix,
        boolean voicedOnset,
        int heavyVowelCount,
        int lightVowelCount,
        BigDecimal heavyVowelRatio) {
}
