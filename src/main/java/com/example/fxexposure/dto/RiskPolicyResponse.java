package com.example.fxexposure.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskPolicyResponse {

    private Long id;
    private String policyName;
    private String currency;
    private BigDecimal maxUnhedgedExposure;
    private BigDecimal minHedgeRatio;
    private BigDecimal maxCounterpartyExposure;
    private BigDecimal warningThresholdPercent;
    private boolean active;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

