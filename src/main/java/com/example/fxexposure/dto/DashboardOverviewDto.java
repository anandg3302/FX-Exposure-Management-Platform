package com.example.fxexposure.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardOverviewDto {

    private String baseCurrency;
    private BigDecimal totalGrossExposure;
    private BigDecimal totalHedgedAmount;
    private BigDecimal totalNetOpenExposure;
    private BigDecimal overallHedgeRatio;
    private BigDecimal portfolioMtM;
    private long totalOpenExposuresCount;
    private long activeHedgesCount;
    private long activeAlertsCount;
    private List<CurrencyExposureDto> currencyBreakdown;
    private List<MaturityBucketDto> maturityLadder;
    private List<ExchangeRateDto> latestRates;
    private List<ComplianceAlertResponse> criticalAlerts;
}

