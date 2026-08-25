package com.example.fxexposure.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.fxexposure.entity.HedgeContract;
import com.example.fxexposure.enums.HedgeStatus;
import com.example.fxexposure.enums.HedgeType;

@Repository
public interface HedgeContractRepository extends JpaRepository<HedgeContract, Long> {

    Optional<HedgeContract> findByDealReference(String dealReference);

    boolean existsByDealReference(String dealReference);

    List<HedgeContract> findByPrimaryCurrencyOrSecondaryCurrency(String primaryCurrency, String secondaryCurrency);

    List<HedgeContract> findByStatus(HedgeStatus status);

    List<HedgeContract> findByStatusNot(HedgeStatus status);

    List<HedgeContract> findByCounterpartyBank(String counterpartyBank);

    List<HedgeContract> findByValueDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT h FROM HedgeContract h WHERE " +
            "(:currency IS NULL OR h.primaryCurrency = :currency OR h.secondaryCurrency = :currency) AND " +
            "(:status IS NULL OR h.status = :status) AND " +
            "(:hedgeType IS NULL OR h.hedgeType = :hedgeType) AND " +
            "(:counterparty IS NULL OR LOWER(h.counterpartyBank) LIKE LOWER(CONCAT('%', :counterparty, '%')))")
    List<HedgeContract> searchHedges(
            @Param("currency") String currency,
            @Param("status") HedgeStatus status,
            @Param("hedgeType") HedgeType hedgeType,
            @Param("counterparty") String counterparty);

    @Query("SELECT DISTINCT h.primaryCurrency FROM HedgeContract h UNION SELECT DISTINCT h.secondaryCurrency FROM HedgeContract h")
    List<String> findDistinctCurrencies();
}

