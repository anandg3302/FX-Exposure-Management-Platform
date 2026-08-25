package com.example.fxexposure.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.fxexposure.enums.RateType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRateDto {

    private Long id;
    private String baseCurrency;
    private String quoteCurrency;
    private BigDecimal rate;
    private RateType rateType;
    private LocalDate rateDate;
    private String source;
    private LocalDateTime updatedAt;
}

