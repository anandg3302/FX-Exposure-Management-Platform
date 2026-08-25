package com.example.fxexposure.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.fxexposure.dto.AllocationRequest;
import com.example.fxexposure.dto.AllocationResponse;
import com.example.fxexposure.entity.Exposure;
import com.example.fxexposure.entity.HedgeAllocation;
import com.example.fxexposure.entity.HedgeContract;
import com.example.fxexposure.enums.ExposureStatus;
import com.example.fxexposure.enums.HedgeStatus;
import com.example.fxexposure.exception.BusinessValidationException;
import com.example.fxexposure.repository.ExposureRepository;
import com.example.fxexposure.repository.HedgeAllocationRepository;
import com.example.fxexposure.repository.HedgeContractRepository;
import com.example.fxexposure.service.impl.AllocationServiceImpl;

@ExtendWith(MockitoExtension.class)
class AllocationServiceTest {

    @Mock
    private HedgeAllocationRepository allocationRepository;

    @Mock
    private ExposureRepository exposureRepository;

    @Mock
    private HedgeContractRepository hedgeRepository;

    @Mock
    private ExposureService exposureService;

    @Mock
    private AuditService auditService;

    private AllocationServiceImpl allocationService;

    @BeforeEach
    void setUp() {
        allocationService = new AllocationServiceImpl(
                allocationRepository, exposureRepository, hedgeRepository, exposureService, auditService);
    }

    @Test
    void testSuccessfulAllocation() {
        Exposure exp = Exposure.builder()
                .id(1L)
                .exposureReference("EXP-EUR-01")
                .currency("EUR")
                .amount(new BigDecimal("100000.00"))
                .hedgedAmount(BigDecimal.ZERO)
                .status(ExposureStatus.UNHEDGED)
                .build();

        HedgeContract hedge = HedgeContract.builder()
                .id(2L)
                .dealReference("HDG-EUR-01")
                .primaryCurrency("EUR")
                .primaryAmount(new BigDecimal("100000.00"))
                .allocatedAmount(BigDecimal.ZERO)
                .strikeRate(new BigDecimal("1.085000"))
                .status(HedgeStatus.ACTIVE)
                .build();

        when(exposureRepository.findById(1L)).thenReturn(Optional.of(exp));
        when(hedgeRepository.findById(2L)).thenReturn(Optional.of(hedge));
        when(allocationRepository.save(any(HedgeAllocation.class))).thenAnswer(i -> {
            HedgeAllocation a = i.getArgument(0);
            a.setId(10L);
            return a;
        });

        AllocationRequest req = AllocationRequest.builder()
                .exposureId(1L)
                .hedgeContractId(2L)
                .allocatedAmount(new BigDecimal("80000.00"))
                .notes("Hedged 80%")
                .build();

        AllocationResponse res = allocationService.allocateHedgeToExposure(req);

        assertNotNull(res);
        assertEquals(new BigDecimal("80000.00"), res.getAllocatedAmount());
        assertEquals(new BigDecimal("80000.00"), exp.getHedgedAmount());
        assertEquals(new BigDecimal("80000.00"), hedge.getAllocatedAmount());
        verify(exposureService).updateExposureHedgingState(exp);
    }

    @Test
    void testCurrencyMismatchThrowsException() {
        Exposure exp = Exposure.builder()
                .id(1L)
                .currency("EUR")
                .amount(new BigDecimal("100000.00"))
                .hedgedAmount(BigDecimal.ZERO)
                .status(ExposureStatus.UNHEDGED)
                .build();

        HedgeContract hedge = HedgeContract.builder()
                .id(2L)
                .primaryCurrency("GBP")
                .primaryAmount(new BigDecimal("100000.00"))
                .allocatedAmount(BigDecimal.ZERO)
                .status(HedgeStatus.ACTIVE)
                .build();

        when(exposureRepository.findById(1L)).thenReturn(Optional.of(exp));
        when(hedgeRepository.findById(2L)).thenReturn(Optional.of(hedge));

        AllocationRequest req = AllocationRequest.builder()
                .exposureId(1L)
                .hedgeContractId(2L)
                .allocatedAmount(new BigDecimal("50000.00"))
                .build();

        assertThrows(BusinessValidationException.class, () -> {
            allocationService.allocateHedgeToExposure(req);
        });
    }

    @Test
    void testOverAllocationThrowsException() {
        Exposure exp = Exposure.builder()
                .id(1L)
                .currency("EUR")
                .amount(new BigDecimal("50000.00"))
                .hedgedAmount(new BigDecimal("40000.00"))
                .status(ExposureStatus.PARTIALLY_HEDGED)
                .build();

        HedgeContract hedge = HedgeContract.builder()
                .id(2L)
                .primaryCurrency("EUR")
                .primaryAmount(new BigDecimal("100000.00"))
                .allocatedAmount(BigDecimal.ZERO)
                .status(HedgeStatus.ACTIVE)
                .build();

        when(exposureRepository.findById(1L)).thenReturn(Optional.of(exp));
        when(hedgeRepository.findById(2L)).thenReturn(Optional.of(hedge));

        // Remaining unhedged on exposure is 10,000, but trying to allocate 20,000
        AllocationRequest req = AllocationRequest.builder()
                .exposureId(1L)
                .hedgeContractId(2L)
                .allocatedAmount(new BigDecimal("20000.00"))
                .build();

        assertThrows(BusinessValidationException.class, () -> {
            allocationService.allocateHedgeToExposure(req);
        });
    }
}

