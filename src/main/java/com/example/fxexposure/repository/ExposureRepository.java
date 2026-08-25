package com.example.fxexposure.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.fxexposure.entity.Exposure;
import com.example.fxexposure.enums.ExposureStatus;
import com.example.fxexposure.enums.ExposureType;

@Repository
public interface ExposureRepository extends JpaRepository<Exposure, Long> {

    Optional<Exposure> findByExposureReference(String exposureReference);

    boolean existsByExposureReference(String exposureReference);

    List<Exposure> findByCurrency(String currency);

    List<Exposure> findByStatus(ExposureStatus status);

    List<Exposure> findByStatusNot(ExposureStatus status);

    List<Exposure> findByValueDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT e FROM Exposure e WHERE " +
            "(:currency IS NULL OR e.currency = :currency) AND " +
            "(:status IS NULL OR e.status = :status) AND " +
            "(:exposureType IS NULL OR e.exposureType = :exposureType) AND " +
            "(:startDate IS NULL OR e.valueDate >= :startDate) AND " +
            "(:endDate IS NULL OR e.valueDate <= :endDate)")
    List<Exposure> searchExposures(
            @Param("currency") String currency,
            @Param("status") ExposureStatus status,
            @Param("exposureType") ExposureType exposureType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT DISTINCT e.currency FROM Exposure e")
    List<String> findDistinctCurrencies();
}

