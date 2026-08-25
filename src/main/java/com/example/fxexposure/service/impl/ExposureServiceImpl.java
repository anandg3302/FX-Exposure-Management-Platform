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

import com.example.fxexposure.dto.ExposureRequest;
import com.example.fxexposure.dto.ExposureResponse;
import com.example.fxexposure.entity.Exposure;
import com.example.fxexposure.enums.ExposureStatus;
import com.example.fxexposure.enums.ExposureType;
import com.example.fxexposure.exception.ResourceNotFoundException;
import com.example.fxexposure.repository.ExposureRepository;
import com.example.fxexposure.repository.HedgeAllocationRepository;
import com.example.fxexposure.security.SecurityUtils;
import com.example.fxexposure.service.AuditService;
import com.example.fxexposure.service.ExchangeRateService;
import com.example.fxexposure.service.ExposureService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExposureServiceImpl implements ExposureService {

    private final ExposureRepository exposureRepository;
    private final HedgeAllocationRepository allocationRepository;
    private final ExchangeRateService exchangeRateService;
    private final AuditService auditService;

    @Value("${app.base-currency:USD}")
    private String defaultBaseCurrency;

    @Override
    @Transactional
    public ExposureResponse createExposure(ExposureRequest request) {
        String reference = request.getExposureReference();
        if (reference == null || reference.trim().isEmpty()) {
            reference = "EXP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        String baseCurr = (request.getBaseCurrency() != null && !request.getBaseCurrency().isEmpty())
                ? request.getBaseCurrency().toUpperCase()
                : defaultBaseCurrency;

        Exposure exposure = Exposure.builder()
                .exposureReference(reference)
                .companyEntity(request.getCompanyEntity())
                .exposureType(request.getExposureType())
                .cashFlowDirection(request.getCashFlowDirection())
                .currency(request.getCurrency().toUpperCase().trim())
                .amount(request.getAmount())
                .baseCurrency(baseCurr)
                .valueDate(request.getValueDate())
                .description(request.getDescription())
                .status(ExposureStatus.UNHEDGED)
                .hedgedAmount(BigDecimal.ZERO)
                .createdBy(SecurityUtils.getCurrentUserEmail())
                .build();

        Exposure saved = exposureRepository.save(exposure);
        auditService.log("CREATE_EXPOSURE", "Exposure", saved.getId(), "Created exposure " + saved.getExposureReference());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ExposureResponse updateExposure(Long id, ExposureRequest request) {
        Exposure exposure = getExposureEntity(id);

        exposure.setCompanyEntity(request.getCompanyEntity());
        exposure.setExposureType(request.getExposureType());
        exposure.setCashFlowDirection(request.getCashFlowDirection());
        exposure.setCurrency(request.getCurrency().toUpperCase().trim());
        exposure.setAmount(request.getAmount());
        exposure.setValueDate(request.getValueDate());
        exposure.setDescription(request.getDescription());

        updateExposureHedgingState(exposure);
        Exposure updated = exposureRepository.save(exposure);

        auditService.log("UPDATE_EXPOSURE", "Exposure", updated.getId(), "Updated exposure " + updated.getExposureReference());
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteExposure(Long id) {
        Exposure exposure = getExposureEntity(id);
        allocationRepository.deleteByExposureId(id);
        exposureRepository.delete(exposure);
        auditService.log("DELETE_EXPOSURE", "Exposure", id, "Deleted exposure " + exposure.getExposureReference());
    }

    @Override
    public ExposureResponse getExposureById(Long id) {
        return mapToResponse(getExposureEntity(id));
    }

    @Override
    public Exposure getExposureEntity(Long id) {
        return exposureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exposure", "id", id));
    }

    @Override
    public List<ExposureResponse> searchExposures(String currency, ExposureStatus status, ExposureType type, LocalDate startDate, LocalDate endDate) {
        return exposureRepository.searchExposures(currency, status, type, startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExposureResponse> getAllExposures() {
        return exposureRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void updateExposureHedgingState(Exposure exposure) {
        BigDecimal hedged = exposure.getHedgedAmount() != null ? exposure.getHedgedAmount() : BigDecimal.ZERO;
        BigDecimal total = exposure.getAmount();

        if (exposure.getStatus() == ExposureStatus.SETTLED || exposure.getStatus() == ExposureStatus.CANCELLED) {
            return;
        }

        if (hedged.compareTo(BigDecimal.ZERO) <= 0) {
            exposure.setStatus(ExposureStatus.UNHEDGED);
        } else if (hedged.compareTo(total) >= 0) {
            exposure.setStatus(ExposureStatus.FULLY_HEDGED);
        } else {
            exposure.setStatus(ExposureStatus.PARTIALLY_HEDGED);
        }
    }

    @Override
    public ExposureResponse mapToResponse(Exposure exposure) {
        BigDecimal hedged = exposure.getHedgedAmount() != null ? exposure.getHedgedAmount() : BigDecimal.ZERO;
        BigDecimal total = exposure.getAmount();
        BigDecimal unhedged = total.subtract(hedged).max(BigDecimal.ZERO);

        BigDecimal hedgeRatio = BigDecimal.ZERO;
        if (total.compareTo(BigDecimal.ZERO) > 0) {
            hedgeRatio = hedged.divide(total, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal amountInBase = BigDecimal.ZERO;
        try {
            amountInBase = exchangeRateService.convertAmount(exposure.getCurrency(), exposure.getBaseCurrency(), exposure.getAmount());
        } catch (Exception ex) {
            log.warn("Could not convert exposure {} amount to base currency", exposure.getExposureReference());
        }

        return ExposureResponse.builder()
                .id(exposure.getId())
                .exposureReference(exposure.getExposureReference())
                .companyEntity(exposure.getCompanyEntity())
                .exposureType(exposure.getExposureType())
                .cashFlowDirection(exposure.getCashFlowDirection())
                .currency(exposure.getCurrency())
                .amount(exposure.getAmount())
                .baseCurrency(exposure.getBaseCurrency())
                .amountInBaseCurrency(amountInBase)
                .valueDate(exposure.getValueDate())
                .description(exposure.getDescription())
                .status(exposure.getStatus())
                .hedgedAmount(hedged)
                .unhedgedAmount(unhedged)
                .hedgeRatioPercent(hedgeRatio)
                .createdBy(exposure.getCreatedBy())
                .createdAt(exposure.getCreatedAt())
                .updatedAt(exposure.getUpdatedAt())
                .build();
    }
}

