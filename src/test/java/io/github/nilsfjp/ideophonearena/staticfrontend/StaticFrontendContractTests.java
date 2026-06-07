package io.github.nilsfjp.ideophonearena.staticfrontend;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StaticFrontendContractTests {

    private static final Path STATIC_DIR = Path.of("src/main/resources/static");

    @Test
    void staticFrontendUsesGameLoopEndpointsAndDtoFields() throws IOException {
        String index = read("index.html");
        String script = read("arena.js");

        assertTrue(index.contains("/arena.css"));
        assertTrue(index.contains("/arena.js"));
        assertTrue(script.contains("/api/auth/"));
        assertTrue(script.contains("/api/game/sessions"));
        assertTrue(script.contains("/rounds/next"));
        assertTrue(script.contains("/answers"));
        assertTrue(script.contains("targetTranslation"));
        assertTrue(script.contains("state.round.completed"));
        assertTrue(script.contains("selectedIdeophoneId"));
        assertTrue(script.contains("responseTimeMs"));
        assertTrue(script.contains("beginTrialSequence"));
        assertTrue(script.contains("cancelTrialSequence"));
        assertTrue(script.contains("isCurrentSequence"));
    }

    @Test
    void staticFrontendContainsScreenshotTrialPhaseOrder() throws IOException {
        String script = read("arena.js");

        int fixation = script.indexOf("state.phase = \"fixation\"");
        int leftPlaying = script.indexOf("state.phase = \"left-playing\"");
        int rightPlaying = script.indexOf("state.phase = \"right-playing\"");
        int choice = script.indexOf("state.phase = \"choice\"");
        int submitting = script.indexOf("state.phase = \"submitting\"");
        int cleanup = script.indexOf("function cancelCurrentPlayback()");

        assertTrue(fixation > 0);
        assertTrue(leftPlaying > fixation);
        assertTrue(rightPlaying > leftPlaying);
        assertTrue(choice > rightPlaying);
        assertTrue(submitting > choice);
        assertTrue(cleanup > 0);
    }

    private String read(String fileName) throws IOException {
        return Files.readString(STATIC_DIR.resolve(fileName), StandardCharsets.UTF_8);
    }
}
