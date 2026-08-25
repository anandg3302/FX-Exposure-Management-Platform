package com.example.fxexposure.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationResponse {

    private Long id;
    private Long exposureId;
    private String exposureReference;
    private String exposureCurrency;
    private BigDecimal exposureAmount;
    private Long hedgeContractId;
    private String dealReference;
    private String hedgePrimaryCurrency;
    private BigDecimal allocatedAmount;
    private BigDecimal effectiveRate;
    private LocalDate allocationDate;
    private String notes;
    private LocalDateTime createdAt;
}

