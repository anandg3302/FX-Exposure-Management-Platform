package com.example.fxexposure.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "System Health", description = "System and service health check endpoint")
public class HealthController {

    @Value("${app.base-currency:USD}")
    private String baseCurrency;

    private static final LocalDateTime START_TIME = LocalDateTime.now();

    @GetMapping("/health")
    @Operation(summary = "Platform health status check")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "application", "FX Exposure Management Platform",
                "version", "1.0.0",
                "baseCurrency", baseCurrency,
                "startTime", START_TIME.toString(),
                "currentTime", LocalDateTime.now().toString(),
                "message", "FX Exposure Management Platform API is running and healthy"
        ));
    }
}
