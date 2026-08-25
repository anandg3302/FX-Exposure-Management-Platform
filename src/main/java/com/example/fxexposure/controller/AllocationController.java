package com.example.fxexposure.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fxexposure.dto.AllocationRequest;
import com.example.fxexposure.dto.AllocationResponse;
import com.example.fxexposure.dto.ApiResponse;
import com.example.fxexposure.service.AllocationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/allocations")
@RequiredArgsConstructor
@Tag(name = "Hedge Allocations", description = "Endpoints for linking and unlinking hedges to underlying exposures")
public class AllocationController {

    private final AllocationService allocationService;

    @GetMapping
    @Operation(summary = "Get all hedge-to-exposure allocations")
    public ResponseEntity<ApiResponse<List<AllocationResponse>>> getAllAllocations() {
        List<AllocationResponse> list = allocationService.getAllAllocations();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Link / Allocate a hedge deal to an exposure (ADMIN / MANAGER only)")
    public ResponseEntity<ApiResponse<AllocationResponse>> allocate(@Valid @RequestBody AllocationRequest request) {
        AllocationResponse response = allocationService.allocateHedgeToExposure(request);
        return new ResponseEntity<>(ApiResponse.ok("Allocation created successfully", response), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Unlink / Deallocate hedge from exposure")
    public ResponseEntity<ApiResponse<Void>> deallocate(@PathVariable Long id) {
        allocationService.deallocate(id);
        return ResponseEntity.ok(ApiResponse.ok("Allocation removed successfully", null));
    }

    @GetMapping("/exposure/{exposureId}")
    @Operation(summary = "Get allocations for specific exposure")
    public ResponseEntity<ApiResponse<List<AllocationResponse>>> getByExposure(@PathVariable Long exposureId) {
        List<AllocationResponse> list = allocationService.getAllocationsForExposure(exposureId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/hedge/{hedgeContractId}")
    @Operation(summary = "Get allocations for specific hedge contract")
    public ResponseEntity<ApiResponse<List<AllocationResponse>>> getByHedge(@PathVariable Long hedgeContractId) {
        List<AllocationResponse> list = allocationService.getAllocationsForHedge(hedgeContractId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }
}

