package com.example.fxexposure.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fxexposure.dto.AllocationRequest;
import com.example.fxexposure.dto.AllocationResponse;
import com.example.fxexposure.entity.Exposure;
import com.example.fxexposure.entity.HedgeAllocation;
import com.example.fxexposure.entity.HedgeContract;
import com.example.fxexposure.enums.HedgeStatus;
import com.example.fxexposure.exception.BusinessValidationException;
import com.example.fxexposure.exception.ResourceNotFoundException;
import com.example.fxexposure.repository.ExposureRepository;
import com.example.fxexposure.repository.HedgeAllocationRepository;
import com.example.fxexposure.repository.HedgeContractRepository;
import com.example.fxexposure.service.AllocationService;
import com.example.fxexposure.service.AuditService;
import com.example.fxexposure.service.ExposureService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllocationServiceImpl implements AllocationService {

    private final HedgeAllocationRepository allocationRepository;
    private final ExposureRepository exposureRepository;
    private final HedgeContractRepository hedgeRepository;
    private final ExposureService exposureService;
    private final AuditService auditService;

    @Override
    @Transactional
    public AllocationResponse allocateHedgeToExposure(AllocationRequest request) {
        Exposure exposure = exposureRepository.findById(request.getExposureId())
                .orElseThrow(() -> new ResourceNotFoundException("Exposure", "id", request.getExposureId()));

        HedgeContract hedge = hedgeRepository.findById(request.getHedgeContractId())
                .orElseThrow(() -> new ResourceNotFoundException("HedgeContract", "id", request.getHedgeContractId()));

        if (hedge.getStatus() != HedgeStatus.ACTIVE && hedge.getStatus() != HedgeStatus.BOOKED) {
            throw new BusinessValidationException("Cannot allocate to a hedge contract with status: " + hedge.getStatus());
        }

        // Validate currency match
        if (!exposure.getCurrency().equalsIgnoreCase(hedge.getPrimaryCurrency())) {
            throw new BusinessValidationException(String.format(
                    "Currency mismatch: Exposure currency (%s) does not match Hedge primary currency (%s)",
                    exposure.getCurrency(), hedge.getPrimaryCurrency()));
        }

        BigDecimal requestedAmount = request.getAllocatedAmount();

        // Check unhedged amount on Exposure
        BigDecimal currentHedged = exposure.getHedgedAmount() != null ? exposure.getHedgedAmount() : BigDecimal.ZERO;
        BigDecimal unhedgedExposure = exposure.getAmount().subtract(currentHedged);
        if (requestedAmount.compareTo(unhedgedExposure) > 0) {
            throw new BusinessValidationException(String.format(
                    "Allocated amount (%s) exceeds remaining unhedged exposure (%s)",
                    requestedAmount, unhedgedExposure));
        }

        // Check unallocated amount on Hedge
        BigDecimal currentAllocated = hedge.getAllocatedAmount() != null ? hedge.getAllocatedAmount() : BigDecimal.ZERO;
        BigDecimal unallocatedHedge = hedge.getPrimaryAmount().subtract(currentAllocated);
        if (requestedAmount.compareTo(unallocatedHedge) > 0) {
            throw new BusinessValidationException(String.format(
                    "Allocated amount (%s) exceeds remaining unallocated hedge capacity (%s)",
                    requestedAmount, unallocatedHedge));
        }

        // Create Allocation
        HedgeAllocation allocation = HedgeAllocation.builder()
                .exposure(exposure)
                .hedgeContract(hedge)
                .allocatedAmount(requestedAmount)
                .effectiveRate(hedge.getStrikeRate())
                .allocationDate(request.getAllocationDate() != null ? request.getAllocationDate() : LocalDate.now())
                .notes(request.getNotes())
                .build();

        HedgeAllocation saved = allocationRepository.save(allocation);

        // Update Exposure
        exposure.setHedgedAmount(currentHedged.add(requestedAmount));
        exposureService.updateExposureHedgingState(exposure);
        exposureRepository.save(exposure);

        // Update Hedge
        hedge.setAllocatedAmount(currentAllocated.add(requestedAmount));
        hedgeRepository.save(hedge);

        auditService.log("ALLOCATE", "HedgeAllocation", saved.getId(),
                String.format("Allocated %s %s from deal %s to exposure %s",
                        requestedAmount, exposure.getCurrency(), hedge.getDealReference(), exposure.getExposureReference()));

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deallocate(Long allocationId) {
        HedgeAllocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("HedgeAllocation", "id", allocationId));

        Exposure exposure = allocation.getExposure();
        HedgeContract hedge = allocation.getHedgeContract();
        BigDecimal amount = allocation.getAllocatedAmount();

        // Restore Exposure hedged amount
        BigDecimal newHedged = exposure.getHedgedAmount().subtract(amount).max(BigDecimal.ZERO);
        exposure.setHedgedAmount(newHedged);
        exposureService.updateExposureHedgingState(exposure);
        exposureRepository.save(exposure);

        // Restore Hedge allocated amount
        BigDecimal newAllocated = hedge.getAllocatedAmount().subtract(amount).max(BigDecimal.ZERO);
        hedge.setAllocatedAmount(newAllocated);
        hedgeRepository.save(hedge);

        allocationRepository.delete(allocation);
        auditService.log("DEALLOCATE", "HedgeAllocation", allocationId,
                String.format("Deallocated %s %s between %s and %s",
                        amount, exposure.getCurrency(), hedge.getDealReference(), exposure.getExposureReference()));
    }

    @Override
    public List<AllocationResponse> getAllocationsForExposure(Long exposureId) {
        return allocationRepository.findByExposureId(exposureId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AllocationResponse> getAllocationsForHedge(Long hedgeContractId) {
        return allocationRepository.findByHedgeContractId(hedgeContractId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AllocationResponse> getAllAllocations() {
        return allocationRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private AllocationResponse mapToResponse(HedgeAllocation allocation) {
        return AllocationResponse.builder()
                .id(allocation.getId())
                .exposureId(allocation.getExposure().getId())
                .exposureReference(allocation.getExposure().getExposureReference())
                .exposureCurrency(allocation.getExposure().getCurrency())
                .exposureAmount(allocation.getExposure().getAmount())
                .hedgeContractId(allocation.getHedgeContract().getId())
                .dealReference(allocation.getHedgeContract().getDealReference())
                .hedgePrimaryCurrency(allocation.getHedgeContract().getPrimaryCurrency())
                .allocatedAmount(allocation.getAllocatedAmount())
                .effectiveRate(allocation.getEffectiveRate())
                .allocationDate(allocation.getAllocationDate())
                .notes(allocation.getNotes())
                .createdAt(allocation.getCreatedAt())
                .build();
    }
}

