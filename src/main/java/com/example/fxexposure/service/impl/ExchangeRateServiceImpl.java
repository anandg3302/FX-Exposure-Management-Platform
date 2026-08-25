package com.example.fxexposure.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fxexposure.dto.ConversionRequest;
import com.example.fxexposure.dto.ConversionResponse;
import com.example.fxexposure.dto.ExchangeRateDto;
import com.example.fxexposure.dto.RateUpdateRequest;
import com.example.fxexposure.entity.ExchangeRate;
import com.example.fxexposure.enums.RateType;
import com.example.fxexposure.exception.ResourceNotFoundException;
import com.example.fxexposure.repository.ExchangeRateRepository;
import com.example.fxexposure.service.ExchangeRateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateServiceImpl implements ExchangeRateService {

    private final ExchangeRateRepository rateRepository;

    @Value("${app.base-currency:USD}")
    private String defaultBaseCurrency;

    @Override
    public BigDecimal getExchangeRate(String baseCurrency, String quoteCurrency) {
        return getExchangeRate(baseCurrency, quoteCurrency, RateType.SPOT);
    }

    @Override
    public BigDecimal getExchangeRate(String baseCurrency, String quoteCurrency, RateType rateType) {
        String base = baseCurrency.toUpperCase().trim();
        String quote = quoteCurrency.toUpperCase().trim();

        if (base.equals(quote)) {
            return BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP);
        }

        // Direct pair: base -> quote
        Optional<ExchangeRate> directRate = rateRepository
                .findTopByBaseCurrencyAndQuoteCurrencyAndRateTypeOrderByRateDateDesc(base, quote, rateType);
        if (directRate.isPresent()) {
            return directRate.get().getRate();
        }

        // Inverse pair: quote -> base (e.g. quote="USD", base="EUR")
        Optional<ExchangeRate> inverseRate = rateRepository
                .findTopByBaseCurrencyAndQuoteCurrencyAndRateTypeOrderByRateDateDesc(quote, base, rateType);
        if (inverseRate.isPresent() && inverseRate.get().getRate().compareTo(BigDecimal.ZERO) > 0) {
            return BigDecimal.ONE.divide(inverseRate.get().getRate(), 6, RoundingMode.HALF_UP);
        }

        // Triangular via default Base Currency (USD)
        if (!base.equals(defaultBaseCurrency) && !quote.equals(defaultBaseCurrency)) {
            try {
                BigDecimal baseToUsd = getExchangeRate(base, defaultBaseCurrency, rateType);
                BigDecimal usdToQuote = getExchangeRate(defaultBaseCurrency, quote, rateType);
                return baseToUsd.multiply(usdToQuote).setScale(6, RoundingMode.HALF_UP);
            } catch (Exception ex) {
                log.warn("Triangular rate lookup failed for {} to {}", base, quote);
            }
        }

        throw new ResourceNotFoundException("Exchange rate not found for pair: " + base + "/" + quote + " [" + rateType + "]");
    }

    @Override
    public ConversionResponse convert(ConversionRequest request) {
        BigDecimal rate = getExchangeRate(request.getFromCurrency(), request.getToCurrency());
        BigDecimal convertedAmount = request.getAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);

        return ConversionResponse.builder()
                .fromCurrency(request.getFromCurrency().toUpperCase())
                .toCurrency(request.getToCurrency().toUpperCase())
                .originalAmount(request.getAmount())
                .convertedAmount(convertedAmount)
                .exchangeRate(rate)
                .build();
    }

    @Override
    public BigDecimal convertAmount(String fromCurrency, String toCurrency, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal rate = getExchangeRate(fromCurrency, toCurrency);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public List<ExchangeRateDto> getLatestRates() {
        return rateRepository.findLatestRatesByType(RateType.SPOT)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ExchangeRateDto updateRate(RateUpdateRequest request) {
        String base = request.getBaseCurrency().toUpperCase().trim();
        String quote = request.getQuoteCurrency().toUpperCase().trim();
        LocalDate date = request.getRateDate() != null ? request.getRateDate() : LocalDate.now();

        ExchangeRate rate = ExchangeRate.builder()
                .baseCurrency(base)
                .quoteCurrency(quote)
                .rate(request.getRate().setScale(6, RoundingMode.HALF_UP))
                .rateType(request.getRateType() != null ? request.getRateType() : RateType.SPOT)
                .rateDate(date)
                .source(request.getSource() != null ? request.getSource() : "MANUAL_ENTRY")
                .build();

        ExchangeRate saved = rateRepository.save(rate);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void seedDefaultRates() {
        if (rateRepository.count() > 0) {
            return;
        }

        LocalDate today = LocalDate.now();

        // Standard Market Spot Rates vs USD
        saveSeedRate("USD", "EUR", new BigDecimal("0.9250"), RateType.SPOT, today);
        saveSeedRate("USD", "GBP", new BigDecimal("0.7850"), RateType.SPOT, today);
        saveSeedRate("USD", "JPY", new BigDecimal("154.6000"), RateType.SPOT, today);
        saveSeedRate("USD", "CAD", new BigDecimal("1.3850"), RateType.SPOT, today);
        saveSeedRate("USD", "AUD", new BigDecimal("1.5200"), RateType.SPOT, today);
        saveSeedRate("USD", "CHF", new BigDecimal("0.8950"), RateType.SPOT, today);
        saveSeedRate("USD", "INR", new BigDecimal("84.2500"), RateType.SPOT, today);
        saveSeedRate("USD", "SGD", new BigDecimal("1.3450"), RateType.SPOT, today);
        saveSeedRate("USD", "CNY", new BigDecimal("7.2450"), RateType.SPOT, today);

        // Forward Curves for key pairs (1M, 3M, 6M)
        saveSeedRate("USD", "EUR", new BigDecimal("0.9230"), RateType.FORWARD_1M, today);
        saveSeedRate("USD", "EUR", new BigDecimal("0.9200"), RateType.FORWARD_3M, today);
        saveSeedRate("USD", "EUR", new BigDecimal("0.9150"), RateType.FORWARD_6M, today);

        saveSeedRate("USD", "GBP", new BigDecimal("0.7830"), RateType.FORWARD_1M, today);
        saveSeedRate("USD", "GBP", new BigDecimal("0.7800"), RateType.FORWARD_3M, today);

        saveSeedRate("USD", "INR", new BigDecimal("84.4500"), RateType.FORWARD_1M, today);
        saveSeedRate("USD", "INR", new BigDecimal("84.8500"), RateType.FORWARD_3M, today);

        log.info("Initialized default FX exchange rates matrix");
    }

    private void saveSeedRate(String base, String quote, BigDecimal rate, RateType type, LocalDate date) {
        rateRepository.save(ExchangeRate.builder()
                .baseCurrency(base)
                .quoteCurrency(quote)
                .rate(rate)
                .rateType(type)
                .rateDate(date)
                .source("INITIAL_SEED")
                .build());
    }

    private ExchangeRateDto mapToDto(ExchangeRate entity) {
        return ExchangeRateDto.builder()
                .id(entity.getId())
                .baseCurrency(entity.getBaseCurrency())
                .quoteCurrency(entity.getQuoteCurrency())
                .rate(entity.getRate())
                .rateType(entity.getRateType())
                .rateDate(entity.getRateDate())
                .source(entity.getSource())
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt() : entity.getCreatedAt())
                .build();
    }
}

