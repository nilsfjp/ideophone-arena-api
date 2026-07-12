package io.github.nilsfjp.ideophonearena.staticfrontend;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.stimuli.locations=file:/code/js/ideophone-arena-web/dist/stimuli/")
@AutoConfigureMockMvc
class StaticResourceHttpTests {

    private static final Path SAMPLE_STIMULUS =
            Path.of("/code/js/ideophone-arena-web/dist/stimuli/a0hu-gosogoso.mp4");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void legacyMiniFrontendIsGone() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/index.html")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/arena.js")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/arena.css")).andExpect(status().isUnauthorized());
    }

    @Test
    void servesStimulusMediaWithoutAuthentication() throws Exception {
        assertTrue(Files.isRegularFile(SAMPLE_STIMULUS), "Sample stimulus file must exist for local demo");

        mockMvc.perform(get("/stimuli/a0hu-gosogoso.mp4"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("video/mp4"));

        mockMvc.perform(head("/stimuli/a0hu-gosogoso.mp4"))
                .andExpect(status().isOk());
    }

    // NIL-108: a nonexistent stimulus must be a 404, not a 500. Boot serves a missing
    // static resource by throwing NoResourceFoundException (since 3.2); without a
    // dedicated handler it falls through to the catch-all Exception handler and
    // surfaces as an internal error.
    @Test
    void missingStimulusIsNotFoundNotServerError() throws Exception {
        mockMvc.perform(get("/stimuli/audio/no-such-stimulus.m4a"))
                .andExpect(status().isNotFound());

        mockMvc.perform(head("/stimuli/audio/no-such-stimulus.m4a"))
                .andExpect(status().isNotFound());
    }

    @Test
    void keepsGameApiProtected() throws Exception {
        mockMvc.perform(post("/api/game/sessions")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
