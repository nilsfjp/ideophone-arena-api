package io.github.nilsfjp.ideophonearena.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// The allow path of the A10 exemption, which the `automation` profile turns on in dev so the
// sanctioned browser loop can register the throwaway browser_loop_* account that fences its
// playthrough out of the frozen research aggregates. The default (property absent) stays
// guarded -- RegistrationHttpTests proves that, since the suite runs under `local` alone.
//
// The exemption is narrow (NIL-90): browser_loop_* only. thesis_p* stays rejected here too,
// because that cohort is read back as data by /api/research/thesis/divergence rather than
// fenced out of it, so a stray thesis_p row is silent corruption of the thesis layer.
@SpringBootTest(properties = "app.automation.allow-browser-loop-registration=true")
@AutoConfigureMockMvc
class RegistrationAutomationHttpTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsReservedBrowserLoopPrefixWhenAutomationFlagOn() throws Exception {
        String username = "browser_loop_" + System.nanoTime();

        String body = register(username)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertNotNull(JsonPath.read(body, "$.token"));
    }

    @Test
    void allowsTheBrowserLoopPrefixUnderTheSameCaseFoldAsTheGuard() throws Exception {
        // The guard rejects THESIS_P37, so the exemption must recognise BROWSER_LOOP_ too --
        // both sides read the username through the same Locale.ROOT fold.
        register("BROWSER_LOOP_" + System.nanoTime()).andExpect(status().isCreated());
    }

    @Test
    void stillRejectsTheThesisCohortPrefixEvenWithTheAutomationFlagOn() throws Exception {
        register("thesis_p99")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.username").exists());
    }

    @Test
    void stillRejectsTheThesisCohortPrefixCaseInsensitivelyWithTheFlagOn() throws Exception {
        register("THESIS_P37")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.username").exists());
    }

    @Test
    void stillAllowsANormalUsernameWithTheFlagOn() throws Exception {
        register("normal_player_" + System.nanoTime()).andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.ResultActions register(String username) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","email":"%s@example.test","password":"password123"}
                        """.formatted(username, username)));
    }
}
