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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.fxexposure.dto.ApiResponse;
import com.example.fxexposure.dto.HedgeContractRequest;
import com.example.fxexposure.dto.HedgeContractResponse;
import com.example.fxexposure.dto.SettlementRequest;
import com.example.fxexposure.enums.HedgeStatus;
import com.example.fxexposure.enums.HedgeType;
import com.example.fxexposure.service.HedgeContractService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/hedges")
@RequiredArgsConstructor
@Tag(name = "Hedge Contracts", description = "Endpoints for booking, revaluing, and settling derivative hedges (Spot, Forward, Options)")
public class HedgeContractController {

    private final HedgeContractService hedgeContractService;

    @GetMapping
    @Operation(summary = "Get all hedge contracts")
    public ResponseEntity<ApiResponse<List<HedgeContractResponse>>> getAllHedges() {
        List<HedgeContractResponse> hedges = hedgeContractService.getAllHedgeContracts();
        return ResponseEntity.ok(ApiResponse.ok(hedges));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get hedge contract by ID")
    public ResponseEntity<ApiResponse<HedgeContractResponse>> getHedgeById(@PathVariable Long id) {
        HedgeContractResponse response = hedgeContractService.getHedgeContractById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Book a new FX hedge contract (ADMIN / MANAGER only)")
    public ResponseEntity<ApiResponse<HedgeContractResponse>> bookHedge(@Valid @RequestBody HedgeContractRequest request) {
        HedgeContractResponse response = hedgeContractService.bookHedgeContract(request);
        return new ResponseEntity<>(ApiResponse.ok("Hedge contract booked successfully", response), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update an existing hedge contract")
    public ResponseEntity<ApiResponse<HedgeContractResponse>> updateHedge(
            @PathVariable Long id, @Valid @RequestBody HedgeContractRequest request) {
        HedgeContractResponse response = hedgeContractService.updateHedgeContract(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Hedge contract updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete a hedge contract")
    public ResponseEntity<ApiResponse<Void>> deleteHedge(@PathVariable Long id) {
        hedgeContractService.deleteHedgeContract(id);
        return ResponseEntity.ok(ApiResponse.ok("Hedge contract deleted successfully", null));
    }

    @GetMapping("/search")
    @Operation(summary = "Search hedge contracts by currency, status, type, and counterparty")
    public ResponseEntity<ApiResponse<List<HedgeContractResponse>>> searchHedges(
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) HedgeStatus status,
            @RequestParam(required = false) HedgeType hedgeType,
            @RequestParam(required = false) String counterparty) {
        List<HedgeContractResponse> results = hedgeContractService.searchHedges(currency, status, hedgeType, counterparty);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @PostMapping("/{id}/settle")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Settle a hedge contract upon maturity, calculating realized gain/loss")
    public ResponseEntity<ApiResponse<HedgeContractResponse>> settleHedge(
            @PathVariable Long id, @RequestBody(required = false) SettlementRequest request) {
        HedgeContractResponse response = hedgeContractService.settleHedgeContract(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Hedge contract settled successfully", response));
    }

    @PostMapping("/{id}/revalue")
    @Operation(summary = "Revalue a single hedge contract Mark-to-Market against current market rates")
    public ResponseEntity<ApiResponse<HedgeContractResponse>> revalueHedge(@PathVariable Long id) {
        HedgeContractResponse response = hedgeContractService.revalueHedgeContract(id);
        return ResponseEntity.ok(ApiResponse.ok("Hedge contract revalued", response));
    }

    @PostMapping("/revalue-all")
    @Operation(summary = "Revalue all active hedge contracts across the portfolio")
    public ResponseEntity<ApiResponse<String>> revalueAllHedges() {
        hedgeContractService.revalueAllActiveHedges();
        return ResponseEntity.ok(ApiResponse.ok("All active hedge contracts revalued successfully", "OK"));
    }
}

