package com.example.fxexposure.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaRResultDto {

    private String baseCurrency;
    private BigDecimal totalPortfolioNotionalInBase;
    private BigDecimal var95Percent; // 95% Confidence Level VaR (1-day / 1-month)
    private BigDecimal var99Percent; // 99% Confidence Level VaR
    private BigDecimal var95PercentOfPortfolio;
    private BigDecimal var99PercentOfPortfolio;
    private int timeHorizonDays;
    private String methodology;
}

