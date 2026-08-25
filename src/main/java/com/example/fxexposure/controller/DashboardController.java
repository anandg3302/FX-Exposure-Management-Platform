package com.example.fxexposure.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fxexposure.dto.ApiResponse;
import com.example.fxexposure.dto.DashboardOverviewDto;
import com.example.fxexposure.service.AnalyticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Executive Treasury Dashboard overview with key KPIs and summaries")
public class DashboardController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    @Operation(summary = "Get full executive dashboard overview with KPIs, rates, maturity ladder, and critical alerts")
    public ResponseEntity<ApiResponse<DashboardOverviewDto>> getOverview() {
        DashboardOverviewDto overview = analyticsService.getDashboardOverview();
        return ResponseEntity.ok(ApiResponse.ok(overview));
    }
}

