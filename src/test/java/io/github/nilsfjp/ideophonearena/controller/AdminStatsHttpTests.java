package io.github.nilsfjp.ideophonearena.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.nilsfjp.ideophonearena.model.AppUser;
import io.github.nilsfjp.ideophonearena.model.enums.Modality;
import io.github.nilsfjp.ideophonearena.model.enums.Role;
import io.github.nilsfjp.ideophonearena.repository.AppUserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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

        // One real CONDITION_1_SOKUON answer makes every aggregate non-trivial
        // and guarantees at least one row in each breakdown.
        submitOneAnswer(token);

        // The pre-promotion token keeps working: the JWT filter reloads the
        // user (and role) from the database on every request.
        String statsJson = mockMvc.perform(get("/api/admin/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                // Totals are shared, mutated by other tests: assert lower bounds only.
                .andExpect(jsonPath("$.totals.users").value(greaterOrEqual(1)))
                .andExpect(jsonPath("$.totals.sessions").value(greaterOrEqual(1)))
                .andExpect(jsonPath("$.totals.completedSessions").value(greaterOrEqual(0)))
                .andExpect(jsonPath("$.totals.answers").value(greaterOrEqual(1)))
                .andExpect(jsonPath("$.byCondition").isArray())
                .andExpect(jsonPath("$.byModality").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Find our own condition row rather than trusting an index: the shared
        // table always contains a CONDITION_1_SOKUON row with answers >= 1.
        List<Map<String, Object>> byCondition = JsonPath.read(statsJson, "$.byCondition");
        Map<String, Object> sokuonRow = byCondition.stream()
                .filter(row -> "CONDITION_1_SOKUON".equals(row.get("conditionName")))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "byCondition is missing a CONDITION_1_SOKUON row: " + byCondition));
        assertTrue(asLong(sokuonRow.get("answers")) >= 1,
                "CONDITION_1_SOKUON answers should be >= 1 but was " + sokuonRow);
        assertTrue(asLong(sokuonRow.get("correct")) >= 0, "correct should be >= 0");
        double conditionAccuracy = asDouble(sokuonRow.get("accuracy"));
        assertTrue(conditionAccuracy >= 0.0 && conditionAccuracy <= 1.0,
                "condition accuracy out of range: " + conditionAccuracy);

        // byModality is a well-formed array: every modality name is a real enum
        // constant and every accuracy sits in [0, 1].
        List<Map<String, Object>> byModality = JsonPath.read(statsJson, "$.byModality");
        assertFalse(byModality.isEmpty(), "byModality should not be empty after an answer");
        Set<String> validModalities = Arrays.stream(Modality.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        for (Map<String, Object> row : byModality) {
            String modality = (String) row.get("modality");
            assertTrue(validModalities.contains(modality),
                    "unknown modality in byModality: " + modality);
            assertTrue(asLong(row.get("answers")) >= 0, "modality answers should be >= 0");
            assertTrue(asLong(row.get("correct")) >= 0, "modality correct should be >= 0");
            double accuracy = asDouble(row.get("accuracy"));
            assertTrue(accuracy >= 0.0 && accuracy <= 1.0,
                    "modality accuracy out of range: " + accuracy);
        }
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

    private static org.hamcrest.Matcher<Integer> greaterOrEqual(int lowerBound) {
        return org.hamcrest.Matchers.greaterThanOrEqualTo(lowerBound);
    }

    private static long asLong(Object value) {
        return ((Number) value).longValue();
    }

    private static double asDouble(Object value) {
        return ((Number) value).doubleValue();
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
