package com.example.fxexposure.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fxexposure.dto.ApiResponse;
import com.example.fxexposure.dto.MaturityBucketDto;
import com.example.fxexposure.dto.NetExposureSummaryDto;
import com.example.fxexposure.dto.ScenarioAnalysisRequest;
import com.example.fxexposure.dto.ScenarioAnalysisResultDto;
import com.example.fxexposure.dto.VaRResultDto;
import com.example.fxexposure.service.AnalyticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Risk & Analytics", description = "Endpoints for Net Open Exposure (NOE), maturity ladder, stress testing, and VaR")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/net-exposure")
    @Operation(summary = "Get Net Open Exposure (NOE) and hedge ratio summary by currency and consolidated in base currency")
    public ResponseEntity<ApiResponse<NetExposureSummaryDto>> getNetExposureSummary() {
        NetExposureSummaryDto summary = analyticsService.getNetExposureSummary();
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/maturity-ladder")
    @Operation(summary = "Get cash flow maturity ladder distributed across standard time buckets (0-30d, 31-60d, 61-90d, 91-180d, 180+d)")
    public ResponseEntity<ApiResponse<List<MaturityBucketDto>>> getMaturityLadder() {
        List<MaturityBucketDto> ladder = analyticsService.getMaturityLadder();
        return ResponseEntity.ok(ApiResponse.ok(ladder));
    }

    @PostMapping("/stress-test")
    @Operation(summary = "Execute 'What-If' scenario stress testing simulating currency percentage shocks across the portfolio")
    public ResponseEntity<ApiResponse<ScenarioAnalysisResultDto>> runStressTest(
            @RequestBody(required = false) ScenarioAnalysisRequest request) {
        ScenarioAnalysisResultDto result = analyticsService.runScenarioStressTest(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/var")
    @Operation(summary = "Calculate 1-Month Parametric Value at Risk (VaR at 95% and 99% confidence levels)")
    public ResponseEntity<ApiResponse<VaRResultDto>> getVaR() {
        VaRResultDto result = analyticsService.calculateVaR();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}

