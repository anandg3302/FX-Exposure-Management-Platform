package com.example.fxexposure.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.fxexposure.dto.ApiResponse;
import com.example.fxexposure.dto.ExposureRequest;
import com.example.fxexposure.dto.ExposureResponse;
import com.example.fxexposure.enums.ExposureStatus;
import com.example.fxexposure.enums.ExposureType;
import com.example.fxexposure.service.ExposureService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/exposures")
@RequiredArgsConstructor
@Tag(name = "Exposures", description = "Endpoints for managing underlying FX exposures (invoices, cash flows, forecasts)")
public class ExposureController {

    private final ExposureService exposureService;

    @GetMapping
    @Operation(summary = "Get all exposures")
    public ResponseEntity<ApiResponse<List<ExposureResponse>>> getAllExposures() {
        List<ExposureResponse> list = exposureService.getAllExposures();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get exposure by ID")
    public ResponseEntity<ApiResponse<ExposureResponse>> getExposureById(@PathVariable Long id) {
        ExposureResponse response = exposureService.getExposureById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ANALYST')")
    @Operation(summary = "Create a new exposure")
    public ResponseEntity<ApiResponse<ExposureResponse>> createExposure(@Valid @RequestBody ExposureRequest request) {
        ExposureResponse response = exposureService.createExposure(request);
        return new ResponseEntity<>(ApiResponse.ok("Exposure created successfully", response), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update an existing exposure")
    public ResponseEntity<ApiResponse<ExposureResponse>> updateExposure(
            @PathVariable Long id, @Valid @RequestBody ExposureRequest request) {
        ExposureResponse response = exposureService.updateExposure(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Exposure updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete an exposure")
    public ResponseEntity<ApiResponse<Void>> deleteExposure(@PathVariable Long id) {
        exposureService.deleteExposure(id);
        return ResponseEntity.ok(ApiResponse.ok("Exposure deleted successfully", null));
    }

    @GetMapping("/search")
    @Operation(summary = "Search & filter exposures by currency, status, type, and date range")
    public ResponseEntity<ApiResponse<List<ExposureResponse>>> searchExposures(
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) ExposureStatus status,
            @RequestParam(required = false) ExposureType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<ExposureResponse> results = exposureService.searchExposures(currency, status, type, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }
}

