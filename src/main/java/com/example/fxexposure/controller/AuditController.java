package com.example.fxexposure.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.fxexposure.dto.ApiResponse;
import com.example.fxexposure.entity.AuditLog;
import com.example.fxexposure.service.AuditService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Endpoints for viewing platform activity and compliance audit trail")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get recent audit activity logs (ADMIN / MANAGER only)")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getRecentLogs() {
        List<AuditLog> logs = auditService.getRecentLogs();
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }

    @GetMapping("/entity")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get audit trail for a specific entity")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getEntityLogs(
            @RequestParam String entityName, @RequestParam Long entityId) {
        List<AuditLog> logs = auditService.getLogsForEntity(entityName, entityId);
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }
}

