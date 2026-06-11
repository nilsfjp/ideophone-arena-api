package io.github.nilsfjp.ideophonearena.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.isA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.nilsfjp.ideophonearena.model.AppUser;
import io.github.nilsfjp.ideophonearena.model.enums.Role;
import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AdminStatsHttpTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void adminStatsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminStatsIsForbiddenForRegularUser() throws Exception {
        String token = registerAndGetToken("admin_stats_user_" + System.nanoTime());

        mockMvc.perform(get("/api/admin/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminStatsReturnsAggregateShapeForAdmin() throws Exception {
        String username = "admin_stats_admin_" + System.nanoTime();
        String token = registerAndGetToken(username);
        promoteToAdmin(username);

        // One real session with one answer makes every aggregate non-trivial.
        submitOneAnswer(token);

        // The pre-promotion token keeps working: the JWT filter reloads the
        // user (and role) from the database on every request.
        mockMvc.perform(get("/api/admin/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.users").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totals.sessions").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totals.completedSessions").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.totals.answers").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.byCondition").value(isA(List.class)))
                .andExpect(jsonPath("$.byCondition[0].conditionName").exists())
                .andExpect(jsonPath("$.byCondition[0].sessions").exists())
                .andExpect(jsonPath("$.byCondition[0].answers").exists())
                .andExpect(jsonPath("$.byCondition[0].correct").exists())
                .andExpect(jsonPath("$.byCondition[0].accuracy").exists())
                .andExpect(jsonPath("$.byModality").value(isA(List.class)))
                .andExpect(jsonPath("$.byModality[0].modality").exists())
                .andExpect(jsonPath("$.byModality[0].answers").exists())
                .andExpect(jsonPath("$.byModality[0].correct").exists())
                .andExpect(jsonPath("$.byModality[0].accuracy").exists());
    }

    @Test
    void seededAdminCanLogIn() throws Exception {
        // Guards the generator-emitted arena_admin row and its frozen hash;
        // the throwaway dev password is documented in docs/demo-runbook.md.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"arena_admin","password":"arena-admin-dev"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    private String registerAndGetToken(String username) throws Exception {
        String authJson = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s@example.test","password":"password123"}
                                """.formatted(username, username)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(authJson, "$.token");
    }

    private void promoteToAdmin(String username) {
        AppUser user = appUserRepository.findByUsername(username).orElseThrow();
        user.setRole(Role.ROLE_ADMIN);
        appUserRepository.save(user);
    }

    private void submitOneAnswer(String token) throws Exception {
        String sessionJson = mockMvc.perform(post("/api/game/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditionName":"CONDITION_1_SOKUON","difficultyLevel":1}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String sessionUuid = JsonPath.read(sessionJson, "$.sessionUuid");

        String roundJson = mockMvc.perform(get("/api/game/sessions/{sessionUuid}/rounds/next", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number roundId = JsonPath.read(roundJson, "$.roundId");
        Number selectedIdeophoneId = JsonPath.read(roundJson, "$.left.ideophoneId");

        mockMvc.perform(post("/api/game/sessions/{sessionUuid}/answers", sessionUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundId":%d,"selectedIdeophoneId":%d,"responseTimeMs":456}
                                """.formatted(roundId.longValue(), selectedIdeophoneId.longValue())))
                .andExpect(status().isOk());
    }
}
