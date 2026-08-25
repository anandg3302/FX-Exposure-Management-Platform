package com.example.fxexposure.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationRequest {

    @NotNull(message = "Exposure ID is required")
    private Long exposureId;

    @NotNull(message = "Hedge Contract ID is required")
    private Long hedgeContractId;

    @NotNull(message = "Allocated amount is required")
    @DecimalMin(value = "0.01", message = "Allocated amount must be greater than zero")
    private BigDecimal allocatedAmount;

    private LocalDate allocationDate;

    private String notes;
}

