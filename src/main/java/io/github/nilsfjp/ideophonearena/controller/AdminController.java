package io.github.nilsfjp.ideophonearena.controller;

import io.github.nilsfjp.ideophonearena.dto.AdminStatsResponse;
import io.github.nilsfjp.ideophonearena.service.AdminStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Research aggregates; requires ROLE_ADMIN")
public class AdminController {

    private final AdminStatsService adminStatsService;

    public AdminController(AdminStatsService adminStatsService) {
        this.adminStatsService = adminStatsService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Aggregate experiment statistics: totals, per condition, per modality")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminStatsService.getStats());
    }
}
