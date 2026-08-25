package com.example.fxexposure.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.fxexposure.entity.ExchangeRate;
import com.example.fxexposure.enums.RateType;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findTopByBaseCurrencyAndQuoteCurrencyAndRateTypeOrderByRateDateDesc(
            String baseCurrency, String quoteCurrency, RateType rateType);

    List<ExchangeRate> findByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(
            String baseCurrency, String quoteCurrency);

    List<ExchangeRate> findByRateDate(LocalDate rateDate);

    @Query("SELECT r FROM ExchangeRate r WHERE r.id IN (" +
            "SELECT MAX(r2.id) FROM ExchangeRate r2 WHERE r2.rateType = :rateType GROUP BY r2.baseCurrency, r2.quoteCurrency)")
    List<ExchangeRate> findLatestRatesByType(@Param("rateType") RateType rateType);

    boolean existsByBaseCurrencyAndQuoteCurrencyAndRateDateAndRateType(
            String baseCurrency, String quoteCurrency, LocalDate rateDate, RateType rateType);
}

