package io.github.nilsfjp.ideophonearena.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.nilsfjp.ideophonearena.dto.ProductionPromptResponse;
import io.github.nilsfjp.ideophonearena.dto.ProductionRequest;
import io.github.nilsfjp.ideophonearena.exception.ConflictException;
import io.github.nilsfjp.ideophonearena.exception.UnparseableInputException;
import io.github.nilsfjp.ideophonearena.mapper.ProductionMapper;
import io.github.nilsfjp.ideophonearena.model.AppUser;
import io.github.nilsfjp.ideophonearena.model.Language;
import io.github.nilsfjp.ideophonearena.model.Production;
import io.github.nilsfjp.ideophonearena.model.Word;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import io.github.nilsfjp.ideophonearena.repository.GameSessionRepository;
import io.github.nilsfjp.ideophonearena.repository.ProductionRepository;
import io.github.nilsfjp.ideophonearena.repository.WordRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class ProductionServiceTests {

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private WordRepository wordRepository;
    @Mock
    private GameSessionRepository gameSessionRepository;
    @Mock
    private ProductionRepository productionRepository;

    private ProductionService productionService;

    private final UserDetails userDetails =
            User.withUsername("minter").password("x").authorities("ROLE_USER").build();
    private AppUser user;

    @BeforeEach
    void setUp() {
        productionService = new ProductionService(appUserRepository, wordRepository, gameSessionRepository,
                productionRepository, new PhonologyService(), new ProductionMapper());
        user = new AppUser("minter", "minter@example.test", "hash");
        setId(user, 7L);
        lenient().when(appUserRepository.findByUsername("minter")).thenReturn(Optional.of(user));
    }

    @Test
    void thePromptCycleRotatesAuditoryVisualHapticInteroceptiveByProductionCount() {
        Word auditory = word(1L, "gosogoso", Modality.AUDITORY);
        Word visual = word(21L, "kukkiri", Modality.VISUAL);
        Word haptic = word(79L, "sarasara", Modality.HAPTIC);
        Word interoceptive = word(41L, "uNzari", Modality.INTEROCEPTIVE);

        stubCandidate(Modality.AUDITORY, auditory);
        stubCandidate(Modality.VISUAL, visual);
        stubCandidate(Modality.HAPTIC, haptic);
        stubCandidate(Modality.INTEROCEPTIVE, interoceptive);

        assertEquals(1L, promptAfter(0).getIdeophoneId());
        assertEquals(21L, promptAfter(1).getIdeophoneId());
        assertEquals(79L, promptAfter(2).getIdeophoneId());
        assertEquals(41L, promptAfter(3).getIdeophoneId());
        // The cursor wraps: the fourth production puts the caller back on AUDITORY.
        assertEquals(1L, promptAfter(4).getIdeophoneId());
    }

    @Test
    void anExhaustedModalityIsSkippedRatherThanBlockingTheCycle() {
        Word visual = word(21L, "kukkiri", Modality.VISUAL);
        stubCandidate(Modality.AUDITORY);
        stubCandidate(Modality.VISUAL, visual);
        stubCandidate(Modality.HAPTIC);
        stubCandidate(Modality.INTEROCEPTIVE);

        // The cursor points at AUDITORY, which is empty; VISUAL is the next non-empty one.
        ProductionPromptResponse prompt = promptAfter(0);
        assertFalse(prompt.isCompleted());
        assertEquals(21L, prompt.getIdeophoneId());
        assertEquals("VISUAL", prompt.getModality());
    }

    @Test
    void everyModalityExhaustedYieldsTheCompletionSentinel() {
        for (Modality modality : List.of(Modality.AUDITORY, Modality.VISUAL, Modality.HAPTIC,
                Modality.INTEROCEPTIVE)) {
            stubCandidate(modality);
        }

        ProductionPromptResponse prompt = promptAfter(94);
        assertTrue(prompt.isCompleted());
        assertNull(prompt.getIdeophoneId());
        assertNull(prompt.getGloss());
        assertNull(prompt.getModality());
    }

    // The one-shot promise: an unparseable form is rejected before the word is even looked
    // up, so nothing is written and the attempt survives.
    @Test
    void unparseableInputNeverTouchesTheRepositories() {
        ProductionRequest request = request(1L, "ngrk");

        assertThrows(UnparseableInputException.class,
                () -> productionService.createProduction(userDetails, request));

        verify(wordRepository, never()).findById(any());
        verify(productionRepository, never()).existsByUserIdAndWordId(any(), any());
        verify(productionRepository, never()).saveAndFlush(any());
    }

    @Test
    void theTargetIsScoredFromItsCanonicalRomajiNotThePlayerPath() {
        // uNzari carries a moraic-nasal marker: routing it through the player normalizer
        // would re-segment it. An exact typed match must still score 100.
        Word target = word(41L, "uNzari", Modality.INTEROCEPTIVE);
        when(wordRepository.findById(41L)).thenReturn(Optional.of(target));
        when(productionRepository.existsByUserIdAndWordId(7L, 41L)).thenReturn(false);

        var response = productionService.createProduction(userDetails, request(41L, "unzari"));

        assertEquals(100, response.getSimilarityScore());
        assertEquals("unzari", response.getInput());
        assertEquals("uNzari", response.getTarget().getRomaji());
        verify(productionRepository).saveAndFlush(any(Production.class));
    }

    // The existsBy pre-check loses the race: a concurrent insert lands between the check and
    // the flush, so the UNIQUE(user_id, word_id) violation must translate to 409, not 500.
    @Test
    void aConcurrentDuplicateSurfacesAsConflictNotAServerError() {
        Word target = word(1L, "gosogoso", Modality.AUDITORY);
        when(wordRepository.findById(1L)).thenReturn(Optional.of(target));
        when(productionRepository.existsByUserIdAndWordId(7L, 1L)).thenReturn(false);
        when(productionRepository.saveAndFlush(any(Production.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(ConflictException.class,
                () -> productionService.createProduction(userDetails, request(1L, "gosogoso")));
    }

    private ProductionPromptResponse promptAfter(long produced) {
        when(productionRepository.countByUserId(7L)).thenReturn(produced);
        return productionService.getNextPrompt(userDetails);
    }

    private void stubCandidate(Modality modality, Word... candidate) {
        lenient().when(productionRepository.findNextUnproducedByModality(eq(7L), eq(modality), any(Pageable.class)))
                .thenReturn(List.of(candidate));
    }

    private ProductionRequest request(long ideophoneId, String input) {
        ProductionRequest request = new ProductionRequest();
        request.setIdeophoneId(ideophoneId);
        request.setInput(input);
        return request;
    }

    private Word word(long id, String romaji, Modality modality) {
        Language japanese = new Language("jpn", "Japanese", "Japonic", null);
        Word word = new Word(japanese, romaji, "kana", "kana", "H", "a gloss", modality,
                "audio/x0h-" + romaji + ".m4a");
        setId(word, id);
        return word;
    }

    private static void setId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
