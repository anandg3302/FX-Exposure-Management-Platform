package com.example.fxexposure.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fxexposure.dto.ApiResponse;
import com.example.fxexposure.dto.ComplianceAlertResponse;
import com.example.fxexposure.security.SecurityUtils;
import com.example.fxexposure.service.ComplianceAlertService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "Compliance Alerts", description = "Endpoints for monitoring and resolving risk policy & limit breach alerts")
public class ComplianceAlertController {

    private final ComplianceAlertService alertService;

    @GetMapping
    @Operation(summary = "Get all compliance alerts")
    public ResponseEntity<ApiResponse<List<ComplianceAlertResponse>>> getAllAlerts() {
        List<ComplianceAlertResponse> alerts = alertService.getAllAlerts();
        return ResponseEntity.ok(ApiResponse.ok(alerts));
    }

    @GetMapping("/active")
    @Operation(summary = "Get currently active compliance alerts")
    public ResponseEntity<ApiResponse<List<ComplianceAlertResponse>>> getActiveAlerts() {
        List<ComplianceAlertResponse> alerts = alertService.getActiveAlerts();
        return ResponseEntity.ok(ApiResponse.ok(alerts));
    }

    @PatchMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge an active compliance alert")
    public ResponseEntity<ApiResponse<ComplianceAlertResponse>> acknowledgeAlert(@PathVariable Long id) {
        String user = SecurityUtils.getCurrentUserEmail();
        ComplianceAlertResponse response = alertService.acknowledgeAlert(id, user);
        return ResponseEntity.ok(ApiResponse.ok("Alert acknowledged", response));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Resolve a compliance alert (ADMIN / MANAGER only)")
    public ResponseEntity<ApiResponse<ComplianceAlertResponse>> resolveAlert(@PathVariable Long id) {
        ComplianceAlertResponse response = alertService.resolveAlert(id);
        return ResponseEntity.ok(ApiResponse.ok("Alert resolved", response));
    }
}

