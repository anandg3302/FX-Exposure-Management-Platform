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
public class MaturityBucketDto {

    private String bucketName; // e.g. "0-30 Days", "31-60 Days", "61-90 Days", "91-180 Days", "180+ Days"
    private int startDays;
    private int endDays;
    private BigDecimal grossInflowsInBase;
    private BigDecimal grossOutflowsInBase;
    private BigDecimal netExposureInBase;
    private BigDecimal hedgedAmountInBase;
    private BigDecimal netOpenExposureInBase;
    private Map<String, BigDecimal> currencyBreakdownInBase;
}

