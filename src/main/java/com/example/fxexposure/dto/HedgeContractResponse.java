package com.example.fxexposure.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.fxexposure.enums.HedgeDirection;
import com.example.fxexposure.enums.HedgeStatus;
import com.example.fxexposure.enums.HedgeType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HedgeContractResponse {

    private Long id;
    private String dealReference;
    private HedgeType hedgeType;
    private HedgeDirection direction;
    private String primaryCurrency;
    private String secondaryCurrency;
    private BigDecimal primaryAmount;
    private BigDecimal secondaryAmount;
    private BigDecimal strikeRate;
    private LocalDate tradeDate;
    private LocalDate valueDate;
    private String counterpartyBank;
    private HedgeStatus status;
    private BigDecimal premiumAmount;
    private BigDecimal currentMtM;
    private BigDecimal currentMarketRate;
    private BigDecimal settledRate;
    private BigDecimal realizedGainLoss;
    private BigDecimal allocatedAmount;
    private BigDecimal unallocatedAmount;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

