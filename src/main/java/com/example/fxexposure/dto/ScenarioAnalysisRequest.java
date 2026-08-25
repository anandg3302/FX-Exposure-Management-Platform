package com.example.fxexposure.dto;

import java.math.BigDecimal;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioAnalysisRequest {

    // Uniform percentage shock applied to all foreign currencies (e.g., +5.0 or -10.0)
    private BigDecimal uniformPercentageShock;

    // Currency-specific percentage shocks (e.g., {"EUR": 5.0, "GBP": -3.5, "JPY": 10.0})
    private Map<String, BigDecimal> currencyShocks;
}

