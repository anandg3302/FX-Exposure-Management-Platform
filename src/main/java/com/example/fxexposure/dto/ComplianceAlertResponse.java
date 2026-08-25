package com.example.fxexposure.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.fxexposure.enums.AlertSeverity;
import com.example.fxexposure.enums.AlertStatus;
import com.example.fxexposure.enums.AlertType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceAlertResponse {

    private Long id;
    private AlertType alertType;
    private AlertSeverity severity;
    private String message;
    private String currency;
    private String relatedEntity;
    private Long relatedEntityId;
    private BigDecimal currentValue;
    private BigDecimal thresholdValue;
    private AlertStatus status;
    private String acknowledgedBy;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}

