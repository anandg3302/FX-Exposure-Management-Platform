package com.example.fxexposure.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.fxexposure.enums.CashFlowDirection;
import com.example.fxexposure.enums.ExposureStatus;
import com.example.fxexposure.enums.ExposureType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExposureResponse {

    private Long id;
    private String exposureReference;
    private String companyEntity;
    private ExposureType exposureType;
    private CashFlowDirection cashFlowDirection;
    private String currency;
    private BigDecimal amount;
    private String baseCurrency;
    private BigDecimal amountInBaseCurrency;
    private LocalDate valueDate;
    private String description;
    private ExposureStatus status;
    private BigDecimal hedgedAmount;
    private BigDecimal unhedgedAmount;
    private BigDecimal hedgeRatioPercent;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

