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
public class NetExposureSummaryDto {

    private String baseCurrency;
    private BigDecimal totalGrossExposureInBase;
    private BigDecimal totalHedgedInBase;
    private BigDecimal totalNetOpenExposureInBase;
    private BigDecimal overallHedgeRatioPercent;
    private BigDecimal totalPortfolioMtMInBase;
    private List<CurrencyExposureDto> currencyBreakdown;
}

