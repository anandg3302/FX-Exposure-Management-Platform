package com.example.fxexposure.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.fxexposure.enums.CashFlowDirection;
import com.example.fxexposure.enums.ExposureType;

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
public class ExposureRequest {

    private String exposureReference;

    @NotBlank(message = "Company entity is required")
    private String companyEntity;

    @NotNull(message = "Exposure type is required")
    private ExposureType exposureType;

    @NotNull(message = "Cash flow direction is required")
    private CashFlowDirection cashFlowDirection;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3-letter ISO code")
    private String currency;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String baseCurrency;

    @NotNull(message = "Value date is required")
    private LocalDate valueDate;

    private String description;
}

