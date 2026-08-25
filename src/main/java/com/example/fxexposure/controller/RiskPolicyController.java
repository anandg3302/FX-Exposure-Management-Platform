package com.example.fxexposure.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fxexposure.dto.ApiResponse;
import com.example.fxexposure.dto.ComplianceAlertResponse;
import com.example.fxexposure.dto.RiskPolicyRequest;
import com.example.fxexposure.dto.RiskPolicyResponse;
import com.example.fxexposure.service.RiskPolicyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/risk-policies")
@RequiredArgsConstructor
@Tag(name = "Risk Policies & Compliance", description = "Endpoints for defining risk limits, minimum hedge ratios, and evaluating compliance")
public class RiskPolicyController {

    private final RiskPolicyService riskPolicyService;

    @GetMapping
    @Operation(summary = "Get all risk policies")
    public ResponseEntity<ApiResponse<List<RiskPolicyResponse>>> getAllPolicies() {
        List<RiskPolicyResponse> list = riskPolicyService.getAllPolicies();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active risk policies")
    public ResponseEntity<ApiResponse<List<RiskPolicyResponse>>> getActivePolicies() {
        List<RiskPolicyResponse> list = riskPolicyService.getActivePolicies();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get risk policy by ID")
    public ResponseEntity<ApiResponse<RiskPolicyResponse>> getPolicyById(@PathVariable Long id) {
        RiskPolicyResponse response = riskPolicyService.getPolicyById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a new risk policy (ADMIN / MANAGER only)")
    public ResponseEntity<ApiResponse<RiskPolicyResponse>> createPolicy(@Valid @RequestBody RiskPolicyRequest request) {
        RiskPolicyResponse response = riskPolicyService.createPolicy(request);
        return new ResponseEntity<>(ApiResponse.ok("Risk policy created successfully", response), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update an existing risk policy")
    public ResponseEntity<ApiResponse<RiskPolicyResponse>> updatePolicy(
            @PathVariable Long id, @Valid @RequestBody RiskPolicyRequest request) {
        RiskPolicyResponse response = riskPolicyService.updatePolicy(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Risk policy updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a risk policy (ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> deletePolicy(@PathVariable Long id) {
        riskPolicyService.deletePolicy(id);
        return ResponseEntity.ok(ApiResponse.ok("Risk policy deleted successfully", null));
    }

    @PostMapping("/check-compliance")
    @Operation(summary = "Trigger a full portfolio compliance check against all active risk policies")
    public ResponseEntity<ApiResponse<List<ComplianceAlertResponse>>> checkCompliance() {
        List<ComplianceAlertResponse> alerts = riskPolicyService.checkCompliance();
        return ResponseEntity.ok(ApiResponse.ok("Compliance check completed", alerts));
    }
}

