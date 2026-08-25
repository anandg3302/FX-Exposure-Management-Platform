package com.example.fxexposure.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementRequest {

    @DecimalMin(value = "0.000001", message = "Settlement rate must be positive")
    private BigDecimal settlementRate;

    private String notes;
}

