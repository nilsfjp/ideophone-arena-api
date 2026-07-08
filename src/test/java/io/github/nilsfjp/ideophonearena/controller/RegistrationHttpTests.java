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

// A10: registration rejects the reserved thesis_p* / browser_loop_* prefixes so a live
// player cannot pollute the research aggregates that fence on them.
@SpringBootTest
@AutoConfigureMockMvc
class RegistrationHttpTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsReservedThesisPrefixWithContractStyleValidationError() throws Exception {
        register("thesis_p99", "thesis_p99@example.test")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.username").exists());
    }

    @Test
    void rejectsReservedBrowserLoopPrefix() throws Exception {
        register("browser_loop_bot", "loopbot@example.test")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.username").exists());
    }

    @Test
    void rejectsReservedPrefixCaseInsensitively() throws Exception {
        // The read-side LIKE fences run under a case-insensitive collation, so THESIS_P37
        // would still match -- registration must reject it too.
        register("THESIS_P37", "upper@example.test")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.username").exists());
    }

    @Test
    void allowsANormalUsername() throws Exception {
        String username = "normal_player_" + System.nanoTime();
        String body = register(username, username + "@example.test")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertNotNull(JsonPath.read(body, "$.token"));
    }

    private org.springframework.test.web.servlet.ResultActions register(String username, String email)
            throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","email":"%s","password":"password123"}
                        """.formatted(username, email)));
    }
}
