package com.example.fxexposure.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fxexposure.dto.ComplianceAlertResponse;
import com.example.fxexposure.entity.ComplianceAlert;
import com.example.fxexposure.enums.AlertSeverity;
import com.example.fxexposure.enums.AlertStatus;
import com.example.fxexposure.enums.AlertType;
import com.example.fxexposure.exception.ResourceNotFoundException;
import com.example.fxexposure.repository.ComplianceAlertRepository;
import com.example.fxexposure.security.SecurityUtils;
import com.example.fxexposure.service.AuditService;
import com.example.fxexposure.service.ComplianceAlertService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceAlertServiceImpl implements ComplianceAlertService {

    private final ComplianceAlertRepository alertRepository;
    private final AuditService auditService;

    @Override
    public List<ComplianceAlertResponse> getActiveAlerts() {
        return alertRepository.findByStatusOrderByCreatedAtDesc(AlertStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ComplianceAlertResponse> getAllAlerts() {
        return alertRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ComplianceAlertResponse createAlert(AlertType alertType, AlertSeverity severity, String message,
                                                String currency, String relatedEntity, Long relatedEntityId,
                                                BigDecimal currentValue, BigDecimal thresholdValue) {
        // Prevent duplicate active alerts for same type & currency
        if (currency != null && alertRepository.existsByAlertTypeAndCurrencyAndStatus(alertType, currency, AlertStatus.ACTIVE)) {
            log.debug("Active alert already exists for type {} and currency {}", alertType, currency);
            return null;
        }

        ComplianceAlert alert = ComplianceAlert.builder()
                .alertType(alertType)
                .severity(severity)
                .message(message)
                .currency(currency)
                .relatedEntity(relatedEntity)
                .relatedEntityId(relatedEntityId)
                .currentValue(currentValue)
                .thresholdValue(thresholdValue)
                .status(AlertStatus.ACTIVE)
                .build();

        ComplianceAlert saved = alertRepository.save(alert);
        auditService.log("CREATE_ALERT", "ComplianceAlert", saved.getId(), "System", "Triggered " + severity + " alert: " + message);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ComplianceAlertResponse acknowledgeAlert(Long id, String username) {
        ComplianceAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ComplianceAlert", "id", id));

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy(username != null ? username : SecurityUtils.getCurrentUserEmail());
        alert.setAcknowledgedAt(LocalDateTime.now());

        ComplianceAlert updated = alertRepository.save(alert);
        auditService.log("ACKNOWLEDGE_ALERT", "ComplianceAlert", id, "Acknowledged alert ID " + id);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public ComplianceAlertResponse resolveAlert(Long id) {
        ComplianceAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ComplianceAlert", "id", id));

        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());

        ComplianceAlert updated = alertRepository.save(alert);
        auditService.log("RESOLVE_ALERT", "ComplianceAlert", id, "Resolved alert ID " + id);
        return mapToResponse(updated);
    }

    private ComplianceAlertResponse mapToResponse(ComplianceAlert alert) {
        return ComplianceAlertResponse.builder()
                .id(alert.getId())
                .alertType(alert.getAlertType())
                .severity(alert.getSeverity())
                .message(alert.getMessage())
                .currency(alert.getCurrency())
                .relatedEntity(alert.getRelatedEntity())
                .relatedEntityId(alert.getRelatedEntityId())
                .currentValue(alert.getCurrentValue())
                .thresholdValue(alert.getThresholdValue())
                .status(alert.getStatus())
                .acknowledgedBy(alert.getAcknowledgedBy())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .resolvedAt(alert.getResolvedAt())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}

