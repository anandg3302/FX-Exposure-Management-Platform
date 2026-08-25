package com.example.fxexposure.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.fxexposure.dto.ConversionRequest;
import com.example.fxexposure.dto.ConversionResponse;
import com.example.fxexposure.entity.ExchangeRate;
import com.example.fxexposure.enums.RateType;
import com.example.fxexposure.exception.ResourceNotFoundException;
import com.example.fxexposure.repository.ExchangeRateRepository;
import com.example.fxexposure.service.impl.ExchangeRateServiceImpl;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateRepository rateRepository;

    private ExchangeRateServiceImpl exchangeRateService;

    @BeforeEach
    void setUp() {
        exchangeRateService = new ExchangeRateServiceImpl(rateRepository);
        ReflectionTestUtils.setField(exchangeRateService, "defaultBaseCurrency", "USD");
    }

    @Test
    void testSameCurrencyRate() {
        BigDecimal rate = exchangeRateService.getExchangeRate("USD", "USD");
        assertEquals(new BigDecimal("1.000000"), rate);
    }

    @Test
    void testDirectRateLookup() {
        ExchangeRate mockRate = ExchangeRate.builder()
                .baseCurrency("USD")
                .quoteCurrency("EUR")
                .rate(new BigDecimal("0.920000"))
                .rateType(RateType.SPOT)
                .rateDate(LocalDate.now())
                .build();

        when(rateRepository.findTopByBaseCurrencyAndQuoteCurrencyAndRateTypeOrderByRateDateDesc(
                eq("USD"), eq("EUR"), eq(RateType.SPOT)))
                .thenReturn(Optional.of(mockRate));

        BigDecimal rate = exchangeRateService.getExchangeRate("USD", "EUR");
        assertEquals(new BigDecimal("0.920000"), rate);
    }

    @Test
    void testInverseRateLookup() {
        ExchangeRate mockRate = ExchangeRate.builder()
                .baseCurrency("USD")
                .quoteCurrency("EUR")
                .rate(new BigDecimal("0.920000"))
                .rateType(RateType.SPOT)
                .rateDate(LocalDate.now())
                .build();

        when(rateRepository.findTopByBaseCurrencyAndQuoteCurrencyAndRateTypeOrderByRateDateDesc(
                eq("EUR"), eq("USD"), eq(RateType.SPOT)))
                .thenReturn(Optional.empty());

        when(rateRepository.findTopByBaseCurrencyAndQuoteCurrencyAndRateTypeOrderByRateDateDesc(
                eq("USD"), eq("EUR"), eq(RateType.SPOT)))
                .thenReturn(Optional.of(mockRate));

        BigDecimal rate = exchangeRateService.getExchangeRate("EUR", "USD");
        BigDecimal expected = BigDecimal.ONE.divide(new BigDecimal("0.920000"), 6, RoundingMode.HALF_UP);
        assertEquals(expected, rate);
    }

    @Test
    void testConversion() {
        ExchangeRate mockRate = ExchangeRate.builder()
                .baseCurrency("USD")
                .quoteCurrency("EUR")
                .rate(new BigDecimal("0.900000"))
                .rateType(RateType.SPOT)
                .rateDate(LocalDate.now())
                .build();

        when(rateRepository.findTopByBaseCurrencyAndQuoteCurrencyAndRateTypeOrderByRateDateDesc(
                eq("USD"), eq("EUR"), eq(RateType.SPOT)))
                .thenReturn(Optional.of(mockRate));

        ConversionRequest req = ConversionRequest.builder()
                .fromCurrency("USD")
                .toCurrency("EUR")
                .amount(new BigDecimal("1000.00"))
                .build();

        ConversionResponse res = exchangeRateService.convert(req);
        assertNotNull(res);
        assertEquals(new BigDecimal("900.00"), res.getConvertedAmount());
    }

    @Test
    void testRateNotFoundThrowsException() {
        when(rateRepository.findTopByBaseCurrencyAndQuoteCurrencyAndRateTypeOrderByRateDateDesc(
                any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            exchangeRateService.getExchangeRate("XYZ", "ABC");
        });
    }
}

