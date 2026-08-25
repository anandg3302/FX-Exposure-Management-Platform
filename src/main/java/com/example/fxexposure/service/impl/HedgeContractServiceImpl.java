package com.example.fxexposure.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fxexposure.dto.HedgeContractRequest;
import com.example.fxexposure.dto.HedgeContractResponse;
import com.example.fxexposure.dto.SettlementRequest;
import com.example.fxexposure.entity.HedgeContract;
import com.example.fxexposure.enums.HedgeDirection;
import com.example.fxexposure.enums.HedgeStatus;
import com.example.fxexposure.enums.HedgeType;
import com.example.fxexposure.enums.RateType;
import com.example.fxexposure.exception.ResourceNotFoundException;
import com.example.fxexposure.repository.HedgeAllocationRepository;
import com.example.fxexposure.repository.HedgeContractRepository;
import com.example.fxexposure.security.SecurityUtils;
import com.example.fxexposure.service.AuditService;
import com.example.fxexposure.service.ExchangeRateService;
import com.example.fxexposure.service.HedgeContractService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HedgeContractServiceImpl implements HedgeContractService {

    private final HedgeContractRepository hedgeRepository;
    private final HedgeAllocationRepository allocationRepository;
    private final ExchangeRateService exchangeRateService;
    private final AuditService auditService;

    @Value("${app.base-currency:USD}")
    private String defaultBaseCurrency;

    @Override
    @Transactional
    public HedgeContractResponse bookHedgeContract(HedgeContractRequest request) {
        String reference = request.getDealReference();
        if (reference == null || reference.trim().isEmpty()) {
            reference = "HDG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        BigDecimal secondaryAmount = request.getSecondaryAmount();
        if (secondaryAmount == null || secondaryAmount.compareTo(BigDecimal.ZERO) == 0) {
            secondaryAmount = request.getPrimaryAmount().multiply(request.getStrikeRate()).setScale(2, RoundingMode.HALF_UP);
        }

        HedgeContract hedge = HedgeContract.builder()
                .dealReference(reference)
                .hedgeType(request.getHedgeType())
                .direction(request.getDirection())
                .primaryCurrency(request.getPrimaryCurrency().toUpperCase().trim())
                .secondaryCurrency(request.getSecondaryCurrency().toUpperCase().trim())
                .primaryAmount(request.getPrimaryAmount())
                .secondaryAmount(secondaryAmount)
                .strikeRate(request.getStrikeRate())
                .tradeDate(request.getTradeDate() != null ? request.getTradeDate() : LocalDate.now())
                .valueDate(request.getValueDate())
                .counterpartyBank(request.getCounterpartyBank())
                .status(HedgeStatus.ACTIVE)
                .premiumAmount(request.getPremiumAmount() != null ? request.getPremiumAmount() : BigDecimal.ZERO)
                .allocatedAmount(BigDecimal.ZERO)
                .currentMtM(BigDecimal.ZERO)
                .createdBy(SecurityUtils.getCurrentUserEmail())
                .build();

        // Calculate initial MtM
        calculateMtM(hedge);

        HedgeContract saved = hedgeRepository.save(hedge);
        auditService.log("BOOK_HEDGE", "HedgeContract", saved.getId(), "Booked deal " + saved.getDealReference());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public HedgeContractResponse updateHedgeContract(Long id, HedgeContractRequest request) {
        HedgeContract hedge = getHedgeContractEntity(id);

        hedge.setHedgeType(request.getHedgeType());
        hedge.setDirection(request.getDirection());
        hedge.setPrimaryCurrency(request.getPrimaryCurrency().toUpperCase().trim());
        hedge.setSecondaryCurrency(request.getSecondaryCurrency().toUpperCase().trim());
        hedge.setPrimaryAmount(request.getPrimaryAmount());
        hedge.setStrikeRate(request.getStrikeRate());
        hedge.setValueDate(request.getValueDate());
        hedge.setCounterpartyBank(request.getCounterpartyBank());

        BigDecimal secondaryAmount = request.getSecondaryAmount();
        if (secondaryAmount == null || secondaryAmount.compareTo(BigDecimal.ZERO) == 0) {
            secondaryAmount = request.getPrimaryAmount().multiply(request.getStrikeRate()).setScale(2, RoundingMode.HALF_UP);
        }
        hedge.setSecondaryAmount(secondaryAmount);

        if (request.getPremiumAmount() != null) {
            hedge.setPremiumAmount(request.getPremiumAmount());
        }

        calculateMtM(hedge);
        HedgeContract updated = hedgeRepository.save(hedge);

        auditService.log("UPDATE_HEDGE", "HedgeContract", updated.getId(), "Updated deal " + updated.getDealReference());
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteHedgeContract(Long id) {
        HedgeContract hedge = getHedgeContractEntity(id);
        allocationRepository.deleteByHedgeContractId(id);
        hedgeRepository.delete(hedge);
        auditService.log("DELETE_HEDGE", "HedgeContract", id, "Deleted deal " + hedge.getDealReference());
    }

    @Override
    public HedgeContractResponse getHedgeContractById(Long id) {
        return mapToResponse(getHedgeContractEntity(id));
    }

    @Override
    public HedgeContract getHedgeContractEntity(Long id) {
        return hedgeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HedgeContract", "id", id));
    }

    @Override
    public List<HedgeContractResponse> searchHedges(String currency, HedgeStatus status, HedgeType hedgeType, String counterparty) {
        return hedgeRepository.searchHedges(currency, status, hedgeType, counterparty)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<HedgeContractResponse> getAllHedgeContracts() {
        return hedgeRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public HedgeContractResponse settleHedgeContract(Long id, SettlementRequest request) {
        HedgeContract hedge = getHedgeContractEntity(id);

        BigDecimal settlementRate = (request != null && request.getSettlementRate() != null)
                ? request.getSettlementRate()
                : exchangeRateService.getExchangeRate(hedge.getSecondaryCurrency(), hedge.getPrimaryCurrency(), RateType.SPOT);

        hedge.setSettledRate(settlementRate);
        hedge.setStatus(HedgeStatus.SETTLED);

        // Realized P&L Calculation:
        BigDecimal notional = hedge.getPrimaryAmount();
        BigDecimal strike = hedge.getStrikeRate();
        BigDecimal realizedPL;

        if (hedge.getHedgeType() == HedgeType.OPTION_CALL) {
            BigDecimal payoff = settlementRate.subtract(strike).max(BigDecimal.ZERO).multiply(notional);
            realizedPL = payoff.subtract(hedge.getPremiumAmount() != null ? hedge.getPremiumAmount() : BigDecimal.ZERO);
        } else if (hedge.getHedgeType() == HedgeType.OPTION_PUT) {
            BigDecimal payoff = strike.subtract(settlementRate).max(BigDecimal.ZERO).multiply(notional);
            realizedPL = payoff.subtract(hedge.getPremiumAmount() != null ? hedge.getPremiumAmount() : BigDecimal.ZERO);
        } else {
            // Spot, Forward, Swaps
            if (hedge.getDirection() == HedgeDirection.BUY) {
                realizedPL = settlementRate.subtract(strike).multiply(notional);
            } else {
                realizedPL = strike.subtract(settlementRate).multiply(notional);
            }
        }

        // Convert realized P&L to base currency
        BigDecimal realizedPLInBase = exchangeRateService.convertAmount(hedge.getSecondaryCurrency(), defaultBaseCurrency, realizedPL);
        hedge.setRealizedGainLoss(realizedPLInBase.setScale(2, RoundingMode.HALF_UP));
        hedge.setCurrentMtM(BigDecimal.ZERO);

        HedgeContract saved = hedgeRepository.save(hedge);
        auditService.log("SETTLE_HEDGE", "HedgeContract", saved.getId(),
                "Settled deal " + saved.getDealReference() + " at rate " + settlementRate + " Realized P&L: " + realizedPLInBase);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public HedgeContractResponse revalueHedgeContract(Long id) {
        HedgeContract hedge = getHedgeContractEntity(id);
        if (hedge.getStatus() == HedgeStatus.ACTIVE || hedge.getStatus() == HedgeStatus.BOOKED) {
            calculateMtM(hedge);
            hedgeRepository.save(hedge);
        }
        return mapToResponse(hedge);
    }

    @Override
    @Transactional
    public void revalueAllActiveHedges() {
        List<HedgeContract> activeHedges = hedgeRepository.findByStatus(HedgeStatus.ACTIVE);
        for (HedgeContract hedge : activeHedges) {
            try {
                calculateMtM(hedge);
                hedgeRepository.save(hedge);
            } catch (Exception ex) {
                log.warn("Failed to revalue hedge contract {}: {}", hedge.getDealReference(), ex.getMessage());
            }
        }
    }

    private void calculateMtM(HedgeContract hedge) {
        if (hedge.getStatus() != HedgeStatus.ACTIVE && hedge.getStatus() != HedgeStatus.BOOKED) {
            return;
        }

        try {
            BigDecimal marketRate = exchangeRateService.getExchangeRate(hedge.getSecondaryCurrency(), hedge.getPrimaryCurrency(), RateType.SPOT);
            BigDecimal strike = hedge.getStrikeRate();
            BigDecimal notional = hedge.getPrimaryAmount();

            BigDecimal mtmInSecondary;
            if (hedge.getHedgeType() == HedgeType.OPTION_CALL) {
                BigDecimal intrinsic = marketRate.subtract(strike).max(BigDecimal.ZERO).multiply(notional);
                mtmInSecondary = intrinsic.subtract(hedge.getPremiumAmount() != null ? hedge.getPremiumAmount() : BigDecimal.ZERO);
            } else if (hedge.getHedgeType() == HedgeType.OPTION_PUT) {
                BigDecimal intrinsic = strike.subtract(marketRate).max(BigDecimal.ZERO).multiply(notional);
                mtmInSecondary = intrinsic.subtract(hedge.getPremiumAmount() != null ? hedge.getPremiumAmount() : BigDecimal.ZERO);
            } else {
                if (hedge.getDirection() == HedgeDirection.BUY) {
                    mtmInSecondary = marketRate.subtract(strike).multiply(notional);
                } else {
                    mtmInSecondary = strike.subtract(marketRate).multiply(notional);
                }
            }

            BigDecimal mtmInBase = exchangeRateService.convertAmount(hedge.getSecondaryCurrency(), defaultBaseCurrency, mtmInSecondary);
            hedge.setCurrentMtM(mtmInBase.setScale(2, RoundingMode.HALF_UP));
        } catch (Exception ex) {
            log.warn("Could not calculate MtM for hedge {}: {}", hedge.getDealReference(), ex.getMessage());
        }
    }

    @Override
    public HedgeContractResponse mapToResponse(HedgeContract hedge) {
        BigDecimal allocated = hedge.getAllocatedAmount() != null ? hedge.getAllocatedAmount() : BigDecimal.ZERO;
        BigDecimal unallocated = hedge.getPrimaryAmount().subtract(allocated).max(BigDecimal.ZERO);

        BigDecimal marketRate = BigDecimal.ZERO;
        try {
            marketRate = exchangeRateService.getExchangeRate(hedge.getSecondaryCurrency(), hedge.getPrimaryCurrency(), RateType.SPOT);
        } catch (Exception ignored) {
        }

        return HedgeContractResponse.builder()
                .id(hedge.getId())
                .dealReference(hedge.getDealReference())
                .hedgeType(hedge.getHedgeType())
                .direction(hedge.getDirection())
                .primaryCurrency(hedge.getPrimaryCurrency())
                .secondaryCurrency(hedge.getSecondaryCurrency())
                .primaryAmount(hedge.getPrimaryAmount())
                .secondaryAmount(hedge.getSecondaryAmount())
                .strikeRate(hedge.getStrikeRate())
                .tradeDate(hedge.getTradeDate())
                .valueDate(hedge.getValueDate())
                .counterpartyBank(hedge.getCounterpartyBank())
                .status(hedge.getStatus())
                .premiumAmount(hedge.getPremiumAmount())
                .currentMtM(hedge.getCurrentMtM())
                .currentMarketRate(marketRate)
                .settledRate(hedge.getSettledRate())
                .realizedGainLoss(hedge.getRealizedGainLoss())
                .allocatedAmount(allocated)
                .unallocatedAmount(unallocated)
                .createdBy(hedge.getCreatedBy())
                .createdAt(hedge.getCreatedAt())
                .updatedAt(hedge.getUpdatedAt())
                .build();
    }
}

