package com.example.fxexposure.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.fxexposure.enums.HedgeDirection;
import com.example.fxexposure.enums.HedgeType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HedgeContractRequest {

    private String dealReference;

    @NotNull(message = "Hedge type is required")
    private HedgeType hedgeType;

    @NotNull(message = "Direction is required")
    private HedgeDirection direction;

    @NotBlank(message = "Primary currency is required")
    @Size(min = 3, max = 3, message = "Primary currency must be 3 letters")
    private String primaryCurrency;

    @NotBlank(message = "Secondary currency is required")
    @Size(min = 3, max = 3, message = "Secondary currency must be 3 letters")
    private String secondaryCurrency;

    @NotNull(message = "Primary amount is required")
    @DecimalMin(value = "0.01", message = "Primary amount must be positive")
    private BigDecimal primaryAmount;

    private BigDecimal secondaryAmount;

    @NotNull(message = "Strike rate is required")
    @DecimalMin(value = "0.000001", message = "Strike rate must be positive")
    private BigDecimal strikeRate;

    @NotNull(message = "Trade date is required")
    private LocalDate tradeDate;

    @NotNull(message = "Value / Maturity date is required")
    private LocalDate valueDate;

    @NotBlank(message = "Counterparty bank is required")
    private String counterpartyBank;

    private BigDecimal premiumAmount;
}

