package com.fpt.swp.controller;

import com.fpt.swp.dto.DashboardStatsDto;
import com.fpt.swp.service.DashboardService;
import com.fpt.swp.util.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final AuthUtils authUtils;

    @GetMapping("/public")
    public ResponseEntity<DashboardStatsDto> getPublicDashboard() {
        return ResponseEntity.ok(dashboardService.getPublicDashboardStats());
    }

    @GetMapping("/me")
    public ResponseEntity<DashboardStatsDto> getUserDashboard(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) {
            return ResponseEntity.ok(dashboardService.getPublicDashboardStats());
        }
        return ResponseEntity.ok(dashboardService.getUserDashboardStats(userId));
    }

    @GetMapping("/activity")
    public ResponseEntity<Map<String, Object>> getUserActivity(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = authUtils.extractUserId(userDetails);
        if (userId == null) {
            return ResponseEntity.ok(Map.of());
        }
        return ResponseEntity.ok(dashboardService.getUserActivityStats(userId));
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        return ResponseEntity.ok(dashboardService.getAdminStats());
    }
}
