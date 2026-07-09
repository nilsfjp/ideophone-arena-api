package io.github.nilsfjp.ideophonearena.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.nilsfjp.ideophonearena.model.Language;
import io.github.nilsfjp.ideophonearena.model.Pairing;
import io.github.nilsfjp.ideophonearena.model.Trial;
import io.github.nilsfjp.ideophonearena.model.Word;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * NIL-62 FE rider. {@code countProducible} answers the {n} of Word Mint's frozen
 * "word {i} of {n}" status line, and two clauses in it are load-bearing. Both are
 * invisible to the service test (which mocks the repository) and to the HTTP test
 * (the seed has no TACTILE/MOTION rows, and no word is trialed twice in a way that
 * would show a join duplicating it). So they are proven here, against the database,
 * by inserting the rows the seed lacks -- otherwise the guard would be a comment.
 */
@SpringBootTest
@Transactional
class CountProducibleTests {

    // ProductionService.PROMPT_CYCLE. Modality also has TACTILE and MOTION, which
    // the cycle never serves.
    private static final List<Modality> CYCLE = List.of(Modality.AUDITORY, Modality.VISUAL,
            Modality.HAPTIC, Modality.INTEROCEPTIVE);

    @Autowired
    private ProductionRepository productionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * The modality fence. Without {@code word.modality in :modalities}, a trialed
     * MOTION word would be counted but never served, so {i} could never reach {n}
     * and the status line would stall one short forever.
     */
    @Test
    void aTrialedWordOutsideTheCycleIsNotProducible() {
        long before = productionRepository.countProducible(CYCLE);

        trialedWord(Modality.MOTION);

        assertEquals(before, productionRepository.countProducible(CYCLE),
                "a trialed MOTION word must not count toward the producible universe");
        // ...and it is otherwise perfectly countable: widen the fence and it appears.
        // Without this the test above would also pass if the word were never persisted.
        assertEquals(before + 1, productionRepository.countProducible(
                        List.of(Modality.AUDITORY, Modality.VISUAL, Modality.HAPTIC,
                                Modality.INTEROCEPTIVE, Modality.MOTION)),
                "the inserted MOTION word should be counted once the fence admits its modality");
    }

    /**
     * Trial membership is an exists subquery, not a join: a word sits in many
     * non-practice trials, and a join would count it once per trial.
     */
    @Test
    void aWordInManyNonPracticeTrialsIsCountedOnce() {
        long before = productionRepository.countProducible(CYCLE);

        Word word = trialedWord(Modality.AUDITORY);
        // A second non-practice trial for the same word, through a second pairing.
        trial(pairing(word, Modality.AUDITORY), word, false);

        assertEquals(before + 1, productionRepository.countProducible(CYCLE),
                "a word in two non-practice trials must be counted exactly once");
    }

    /** Practice is a trial fact (ADR-3): practice-only words are never served. */
    @Test
    void aPracticeOnlyWordIsNotProducible() {
        long before = productionRepository.countProducible(CYCLE);

        Word word = word(Modality.AUDITORY);
        trial(pairing(word, Modality.AUDITORY), word, true);

        assertEquals(before, productionRepository.countProducible(CYCLE),
                "a word that appears only in practice trials must not be producible");
    }

    private Word trialedWord(Modality modality) {
        Word word = word(modality);
        trial(pairing(word, modality), word, false);
        return word;
    }

    // `words` is UNIQUE(language_id, romaji) and `pair_code` is 20 chars, so both
    // are minted from a counter rather than from real (already-seeded) forms.
    private int fixtureCount;

    private Word word(Modality modality) {
        fixtureCount += 1;
        String romaji = "nil62w" + fixtureCount;
        Language japanese = entityManager
                .createQuery("select language from Language language where language.isoCode = 'jpn'", Language.class)
                .getSingleResult();
        Word word = new Word(japanese, romaji, "かな", "かな", "H", "a gloss", modality,
                "audio/x0h-" + romaji + ".m4a");
        entityManager.persist(word);
        return word;
    }

    private Pairing pairing(Word word, Modality modality) {
        // The foil is wordB of a non-practice pairing, so the query sees it too. Mint it
        // TACTILE -- outside the cycle -- so it never perturbs the counts below, which is
        // itself one more demonstration that the modality fence bites.
        Word foil = word(Modality.TACTILE);
        Pairing pairing = new Pairing("nil62p" + fixtureCount, word.getLanguage(), word, foil,
                modality, false, "test");
        entityManager.persist(pairing);
        return pairing;
    }

    private void trial(Pairing pairing, Word correctWord, boolean practice) {
        entityManager.persist(new Trial(pairing, correctWord, practice));
        entityManager.flush();
    }
}
