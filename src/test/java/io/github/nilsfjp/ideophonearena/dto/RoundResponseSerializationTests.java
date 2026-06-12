package io.github.nilsfjp.ideophonearena.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.nilsfjp.ideophonearena.model.enums.ConditionName;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class RoundResponseSerializationTests {

    @Test
    void exposesTargetTranslationWithoutChoiceGlosses() throws Exception {
        IdeophoneChoiceResponse left = new IdeophoneChoiceResponse(
                1L,
                "left-kana",
                "left-display",
                "left-canonical",
                "left-romaji",
                "audio/left.m4a",
                "/stimuli/audio/left.m4a",
                Modality.AUDITORY,
                "HU"
        );
        IdeophoneChoiceResponse right = new IdeophoneChoiceResponse(
                2L,
                "right-kana",
                "right-display",
                "right-canonical",
                "right-romaji",
                "audio/right.m4a",
                "/stimuli/audio/right.m4a",
                Modality.AUDITORY,
                "KD"
        );
        RoundResponse response = new RoundResponse(
                "session-uuid",
                10L,
                "target meaning",
                ConditionName.CONDITION_1_SOKUON,
                1,
                false,
                new TranslationResponse("target meaning", "distractor meaning"),
                left,
                right,
                new TimingResponse(800, 0)
        );

        Set<String> roundProperties = beanProperties(RoundResponse.class);
        Set<String> choiceProperties = beanProperties(IdeophoneChoiceResponse.class);

        assertEquals("target meaning", response.getTargetTranslation());
        assertEquals("target meaning", response.getPrompt());
        assertFalse(response.isCompleted());
        assertTrue(roundProperties.contains("targetTranslation"));
        assertTrue(roundProperties.contains("prompt"));
        assertTrue(roundProperties.contains("completed"));
        assertTrue(roundProperties.contains("message"));
        assertTrue(roundProperties.contains("practice"));
        assertFalse(response.isPractice());
        assertFalse(choiceProperties.contains("gloss"));
        assertTrue(choiceProperties.contains("stimulusUrl"));
        assertTrue(choiceProperties.contains("displayForm"));
        assertTrue(choiceProperties.contains("canonicalForm"));
        assertEquals("left-display", left.getDisplayForm());
        assertEquals("left-canonical", left.getCanonicalForm());
    }

    private Set<String> beanProperties(Class<?> type) throws IntrospectionException {
        return Arrays.stream(Introspector.getBeanInfo(type).getPropertyDescriptors())
                .map(descriptor -> descriptor.getName())
                .collect(Collectors.toSet());
    }
}
