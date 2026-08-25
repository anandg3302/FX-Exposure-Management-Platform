package com.example.fxexposure.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fxexposure.dto.ApiResponse;
import com.example.fxexposure.dto.ConversionRequest;
import com.example.fxexposure.dto.ConversionResponse;
import com.example.fxexposure.dto.ExchangeRateDto;
import com.example.fxexposure.dto.RateUpdateRequest;
import com.example.fxexposure.service.ExchangeRateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rates")
@RequiredArgsConstructor
@Tag(name = "Exchange Rates", description = "Endpoints for market spot & forward FX rates and currency conversion")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping
    @Operation(summary = "Get latest exchange rates matrix")
    public ResponseEntity<ApiResponse<List<ExchangeRateDto>>> getLatestRates() {
        List<ExchangeRateDto> rates = exchangeRateService.getLatestRates();
        return ResponseEntity.ok(ApiResponse.ok(rates));
    }

    @PostMapping("/update")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update or add an exchange rate")
    public ResponseEntity<ApiResponse<ExchangeRateDto>> updateRate(@Valid @RequestBody RateUpdateRequest request) {
        ExchangeRateDto updated = exchangeRateService.updateRate(request);
        return ResponseEntity.ok(ApiResponse.ok("Exchange rate updated successfully", updated));
    }

    @PostMapping("/convert")
    @Operation(summary = "Perform real-time currency conversion")
    public ResponseEntity<ApiResponse<ConversionResponse>> convert(@Valid @RequestBody ConversionRequest request) {
        ConversionResponse response = exchangeRateService.convert(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

