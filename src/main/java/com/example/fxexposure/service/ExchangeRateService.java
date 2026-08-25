package com.example.fxexposure.service;

import java.math.BigDecimal;
import java.util.List;

import com.example.fxexposure.dto.ConversionRequest;
import com.example.fxexposure.dto.ConversionResponse;
import com.example.fxexposure.dto.ExchangeRateDto;
import com.example.fxexposure.dto.RateUpdateRequest;
import com.example.fxexposure.enums.RateType;

public interface ExchangeRateService {

    BigDecimal getExchangeRate(String baseCurrency, String quoteCurrency);

    BigDecimal getExchangeRate(String baseCurrency, String quoteCurrency, RateType rateType);

    ConversionResponse convert(ConversionRequest request);

    BigDecimal convertAmount(String fromCurrency, String toCurrency, BigDecimal amount);

    List<ExchangeRateDto> getLatestRates();

    ExchangeRateDto updateRate(RateUpdateRequest request);

    void seedDefaultRates();
}

