package com.drone.collab.controller;

import com.drone.collab.common.ApiResponse;
import com.drone.collab.service.StatsApiService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public/stats")
public class StatsController {

    private final StatsApiService statsApiService;

    public StatsController(StatsApiService statsApiService) {
        this.statsApiService = statsApiService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'PILOT', 'ORG')")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(statsApiService.dashboard());
    }
}
