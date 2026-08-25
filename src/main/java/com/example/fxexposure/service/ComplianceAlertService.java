package com.example.fxexposure.service;

import java.math.BigDecimal;
import java.util.List;

import com.example.fxexposure.dto.ComplianceAlertResponse;
import com.example.fxexposure.enums.AlertSeverity;
import com.example.fxexposure.enums.AlertType;

public interface ComplianceAlertService {

    List<ComplianceAlertResponse> getActiveAlerts();

    List<ComplianceAlertResponse> getAllAlerts();

    ComplianceAlertResponse createAlert(AlertType alertType, AlertSeverity severity, String message,
                                        String currency, String relatedEntity, Long relatedEntityId,
                                        BigDecimal currentValue, BigDecimal thresholdValue);

    ComplianceAlertResponse acknowledgeAlert(Long id, String username);

    ComplianceAlertResponse resolveAlert(Long id);
}

