package io.github.nilsfjp.ideophonearena.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
// browser_loop_* only: it is Rider-A-excluded everywhere, so proving the exemption with it
// leaves no residue. A thesis_p* row would seed a fake member of the ingestion cohort.
@SpringBootTest(properties = "app.automation.allow-reserved-registration=true")
@AutoConfigureMockMvc
class RegistrationAutomationHttpTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsReservedBrowserLoopPrefixWhenAutomationFlagOn() throws Exception {
        String username = "browser_loop_" + System.nanoTime();
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s@example.test","password":"password123"}
                                """.formatted(username, username)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertNotNull(JsonPath.read(body, "$.token"));
    }
}
