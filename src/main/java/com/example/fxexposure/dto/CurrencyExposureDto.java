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
public class CurrencyExposureDto {

    private String currency;
    private BigDecimal grossInflows;
    private BigDecimal grossOutflows;
    private BigDecimal grossExposure;
    private BigDecimal hedgedAmount;
    private BigDecimal netOpenExposure;
    private BigDecimal hedgeRatioPercent;
    private BigDecimal spotRateToBase;
    private BigDecimal netOpenExposureInBase;
    private BigDecimal grossExposureInBase;
    private BigDecimal hedgedAmountInBase;
}

