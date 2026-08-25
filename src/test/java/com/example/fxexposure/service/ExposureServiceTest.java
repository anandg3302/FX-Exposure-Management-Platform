package com.example.fxexposure.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.fxexposure.dto.ExposureRequest;
import com.example.fxexposure.dto.ExposureResponse;
import com.example.fxexposure.entity.Exposure;
import com.example.fxexposure.enums.CashFlowDirection;
import com.example.fxexposure.enums.ExposureStatus;
import com.example.fxexposure.enums.ExposureType;
import com.example.fxexposure.repository.ExposureRepository;
import com.example.fxexposure.repository.HedgeAllocationRepository;
import com.example.fxexposure.service.impl.ExposureServiceImpl;

@ExtendWith(MockitoExtension.class)
class ExposureServiceTest {

    @Mock
    private ExposureRepository exposureRepository;

    @Mock
    private HedgeAllocationRepository allocationRepository;

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private AuditService auditService;

    private ExposureServiceImpl exposureService;

    @BeforeEach
    void setUp() {
        exposureService = new ExposureServiceImpl(exposureRepository, allocationRepository, exchangeRateService, auditService);
        ReflectionTestUtils.setField(exposureService, "defaultBaseCurrency", "USD");
    }

    @Test
    void testCreateExposure() {
        ExposureRequest req = ExposureRequest.builder()
                .companyEntity("Acme Germany")
                .exposureType(ExposureType.RECEIVABLE_INVOICE)
                .cashFlowDirection(CashFlowDirection.INFLOW)
                .currency("EUR")
                .amount(new BigDecimal("500000.00"))
                .valueDate(LocalDate.now().plusDays(30))
                .description("Machinery Export")
                .build();

        when(exposureRepository.save(any(Exposure.class))).thenAnswer(invocation -> {
            Exposure exp = invocation.getArgument(0);
            exp.setId(1L);
            return exp;
        });

        when(exchangeRateService.convertAmount(any(), any(), any())).thenReturn(new BigDecimal("550000.00"));

        ExposureResponse res = exposureService.createExposure(req);

        assertNotNull(res);
        assertEquals("Acme Germany", res.getCompanyEntity());
        assertEquals("EUR", res.getCurrency());
        assertEquals(ExposureStatus.UNHEDGED, res.getStatus());
        assertEquals(new BigDecimal("500000.00"), res.getAmount());
        verify(exposureRepository).save(any(Exposure.class));
    }

    @Test
    void testUpdateHedgingState() {
        Exposure exp = Exposure.builder()
                .amount(new BigDecimal("100000.00"))
                .hedgedAmount(BigDecimal.ZERO)
                .status(ExposureStatus.UNHEDGED)
                .build();

        exposureService.updateExposureHedgingState(exp);
        assertEquals(ExposureStatus.UNHEDGED, exp.getStatus());

        exp.setHedgedAmount(new BigDecimal("40000.00"));
        exposureService.updateExposureHedgingState(exp);
        assertEquals(ExposureStatus.PARTIALLY_HEDGED, exp.getStatus());

        exp.setHedgedAmount(new BigDecimal("100000.00"));
        exposureService.updateExposureHedgingState(exp);
        assertEquals(ExposureStatus.FULLY_HEDGED, exp.getStatus());
    }

    @Test
    void testDeleteExposure() {
        Exposure exp = Exposure.builder()
                .id(1L)
                .exposureReference("EXP-001")
                .build();

        when(exposureRepository.findById(1L)).thenReturn(Optional.of(exp));

        exposureService.deleteExposure(1L);

        verify(allocationRepository).deleteByExposureId(1L);
        verify(exposureRepository).delete(exp);
    }
}

