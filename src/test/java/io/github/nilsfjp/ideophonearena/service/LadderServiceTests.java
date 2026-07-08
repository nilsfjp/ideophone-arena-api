package io.github.nilsfjp.ideophonearena.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.nilsfjp.ideophonearena.dto.LadderFloorResponse;
import io.github.nilsfjp.ideophonearena.dto.LadderFloorsResponse;
import io.github.nilsfjp.ideophonearena.mapper.LadderMapper;
import io.github.nilsfjp.ideophonearena.model.AppUser;
import io.github.nilsfjp.ideophonearena.model.Pairing;
import io.github.nilsfjp.ideophonearena.model.Trial;
import io.github.nilsfjp.ideophonearena.model.Word;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import io.github.nilsfjp.ideophonearena.repository.LadderFloorScoreProjection;
import io.github.nilsfjp.ideophonearena.repository.PlayerAnswerRepository;
import io.github.nilsfjp.ideophonearena.repository.TrialRepository;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class LadderServiceTests {

    private static final String USERNAME = "climber";

    @Mock
    private TrialRepository trialRepository;

    @Mock
    private PlayerAnswerRepository playerAnswerRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private UserDetails userDetails;

    private final LadderFloors ladderFloors = new LadderFloors();
    private LadderService ladderService;
    private long nextId = 1L;

    @BeforeEach
    void setUp() {
        ladderService = new LadderService(ladderFloors, trialRepository, playerAnswerRepository, appUserRepository,
                new LadderMapper());
        AppUser user = new AppUser(USERNAME, "climber@example.test", "hash");
        setId(user, 7L);
        when(userDetails.getUsername()).thenReturn(USERNAME);
        when(appUserRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
    }

    @Test
    void returnsFourFloorsInHierarchyOrderWhenAllHaveTrials() {
        when(trialRepository.findScoredTrialsByPairCodes(any())).thenReturn(trialsForAllFloors());
        when(playerAnswerRepository.findCompletedLadderSessionScores(any())).thenReturn(List.of());

        LadderFloorsResponse response = ladderService.getFloors(userDetails);

        List<LadderFloorResponse> floors = response.getFloors();
        assertEquals(List.of(Modality.AUDITORY, Modality.VISUAL, Modality.HAPTIC, Modality.INTEROCEPTIVE),
                floors.stream().map(LadderFloorResponse::getModality).toList(),
                "floors come back in Sound -> Sight -> Touch -> Inner states order");
        LadderFloorResponse touch = floors.get(2);
        assertEquals(Modality.HAPTIC, touch.getModality());
        assertEquals(4, touch.getPairCount(), "the Touch floor is a 4-pair floor");
        assertEquals("exp-h6", touch.getFinalRungPairCode());
        assertEquals(4, touch.getPairs().size());
        assertTrue(touch.getPairs().get(3).isFinalRung(), "the last rung is the final rung");
        assertFalse(touch.getPairs().get(0).isFinalRung());
        assertEquals(10, floors.get(0).getPairCount(), "the Sound floor is a 10-pair floor");
        assertEquals(List.of("a9", "a7", "a2", "a0", "a3", "a6", "a4", "a8", "a1", "a5"),
                floors.get(0).getPairs().stream().map(p -> p.getPairCode()).toList(),
                "within-floor order is thesis-facts section 8");
    }

    // Data-driven presence + zero code delta: with no HAPTIC trials the Touch floor simply
    // does not appear -- and when its trials exist it slots back in at hierarchy position 3
    // with no code change (see returnsFourFloors... above).
    @Test
    void touchFloorIsAbsentWhenItsTrialsAreDark() {
        List<Trial> aviOnly = trialsForAllFloors().stream()
                .filter(trial -> trial.getPairing().getModality() != Modality.HAPTIC)
                .toList();
        when(trialRepository.findScoredTrialsByPairCodes(any())).thenReturn(aviOnly);
        when(playerAnswerRepository.findCompletedLadderSessionScores(any())).thenReturn(List.of());

        LadderFloorsResponse response = ladderService.getFloors(userDetails);

        assertEquals(List.of(Modality.AUDITORY, Modality.VISUAL, Modality.INTEROCEPTIVE),
                response.getFloors().stream().map(LadderFloorResponse::getModality).toList(),
                "with dark HAPTIC trials the ladder is a 3-floor A/V/I climb");
    }

    @Test
    void clearedFloorCarriesTheCallersBestSessionScore() {
        when(trialRepository.findScoredTrialsByPairCodes(any())).thenReturn(trialsForAllFloors());
        when(playerAnswerRepository.findCompletedLadderSessionScores(any())).thenReturn(List.of(
                score(Modality.AUDITORY, 10, 6),
                score(Modality.AUDITORY, 10, 8),   // best correct wins
                score(Modality.HAPTIC, 4, 3)));

        LadderFloorsResponse response = ladderService.getFloors(userDetails);

        LadderFloorResponse sound = response.getFloors().get(0);
        assertTrue(sound.isCleared());
        assertEquals(8, sound.getBestCorrect());
        assertEquals(10, sound.getBestAnswered());

        LadderFloorResponse sight = response.getFloors().get(1);
        assertFalse(sight.isCleared(), "an unplayed floor is not cleared");
        assertNull(sight.getBestCorrect());
        assertNull(sight.getBestAnswered());

        LadderFloorResponse touch = response.getFloors().get(2);
        assertTrue(touch.isCleared());
        assertEquals(3, touch.getBestCorrect());
    }

    private List<Trial> trialsForAllFloors() {
        List<Trial> trials = new ArrayList<>();
        for (LadderFloors.Floor floor : ladderFloors.hierarchy()) {
            for (String pairCode : floor.getPairCodes()) {
                trials.add(trialForCode(pairCode, floor.getModality()));
            }
        }
        return trials;
    }

    private Trial trialForCode(String pairCode, Modality modality) {
        Word wordA = word(modality);
        Word wordB = word(modality);
        Pairing pairing = new Pairing(pairCode, null, wordA, wordB, modality, true, "THESIS");
        Trial trial = new Trial(pairing, wordA, false);
        setId(trial, nextId++);
        return trial;
    }

    private Word word(Modality modality) {
        Word word = new Word(null, "romaji" + nextId, "kana", "kana", "H", "gloss", modality, "audio/x.m4a");
        setId(word, nextId++);
        return word;
    }

    private static LadderFloorScoreProjection score(Modality floor, long answered, long correct) {
        return new LadderFloorScoreProjection() {
            @Override
            public Modality getFloor() {
                return floor;
            }

            @Override
            public Long getAnswered() {
                return answered;
            }

            @Override
            public Long getCorrect() {
                return correct;
            }
        };
    }

    private void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new IllegalStateException("Could not set test id", exception);
        }
    }
}
