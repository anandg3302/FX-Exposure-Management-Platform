package com.example.fxexposure.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskPolicyRequest {

    @NotBlank(message = "Policy name is required")
    private String policyName;

    @Builder.Default
    private String currency = "ALL";

    @NotNull(message = "Max unhedged exposure is required")
    @DecimalMin(value = "0.00", message = "Max unhedged exposure must be non-negative")
    private BigDecimal maxUnhedgedExposure;

    @NotNull(message = "Min hedge ratio is required")
    @DecimalMin(value = "0.00", message = "Min hedge ratio must be non-negative")
    @DecimalMax(value = "100.00", message = "Min hedge ratio cannot exceed 100%")
    private BigDecimal minHedgeRatio;

    @NotNull(message = "Max counterparty exposure is required")
    @DecimalMin(value = "0.00", message = "Max counterparty exposure must be non-negative")
    private BigDecimal maxCounterpartyExposure;

    @Builder.Default
    private BigDecimal warningThresholdPercent = new BigDecimal("80.00");

    @Builder.Default
    private boolean active = true;

    private String description;
}

