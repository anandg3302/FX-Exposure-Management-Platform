package com.example.fxexposure.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.fxexposure.enums.RateType;

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
public class RateUpdateRequest {

    @NotBlank(message = "Base currency is required")
    @Size(min = 3, max = 3, message = "Base currency must be 3-letter ISO code")
    private String baseCurrency;

    @NotBlank(message = "Quote currency is required")
    @Size(min = 3, max = 3, message = "Quote currency must be 3-letter ISO code")
    private String quoteCurrency;

    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.000001", message = "Rate must be positive")
    private BigDecimal rate;

    @Builder.Default
    private RateType rateType = RateType.SPOT;

    private LocalDate rateDate;

    @Builder.Default
    private String source = "MANUAL_ENTRY";
}

