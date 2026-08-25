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
public class ScenarioAnalysisResultDto {

    private String baseCurrency;
    private BigDecimal baselinePortfolioValueInBase;
    private BigDecimal stressedPortfolioValueInBase;
    private BigDecimal exposureGainLossInBase;
    private BigDecimal hedgeGainLossInBase;
    private BigDecimal netPortfolioImpactInBase;
    private BigDecimal netImpactPercent;
    private Map<String, BigDecimal> currencyImpactsInBase;
    private String summary;
}

