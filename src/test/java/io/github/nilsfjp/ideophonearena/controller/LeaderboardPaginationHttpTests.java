package io.github.nilsfjp.ideophonearena.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LeaderboardPaginationHttpTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void leaderboardReturnsPaginationWrapperWithDefaults() throws Exception {
        mockMvc.perform(get("/api/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries").isArray())
                .andExpect(jsonPath("$.entries.length()").value(lessThanOrEqualTo(10)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.totalPages").value(greaterThanOrEqualTo(0)));
    }

    @Test
    void leaderboardCapsSizeAtFifty() throws Exception {
        mockMvc.perform(get("/api/leaderboard").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    void leaderboardHonorsExplicitPageAndSize() throws Exception {
        mockMvc.perform(get("/api/leaderboard").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.entries.length()").value(lessThanOrEqualTo(5)));
    }

    @Test
    void leaderboardClampsNegativeOrZeroParams() throws Exception {
        mockMvc.perform(get("/api/leaderboard").param("page", "-1").param("size", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1));
    }
}
