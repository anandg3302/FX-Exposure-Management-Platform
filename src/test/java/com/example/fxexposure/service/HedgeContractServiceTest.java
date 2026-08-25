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
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.fxexposure.dto.HedgeContractRequest;
import com.example.fxexposure.dto.HedgeContractResponse;
import com.example.fxexposure.dto.SettlementRequest;
import com.example.fxexposure.entity.HedgeContract;
import com.example.fxexposure.enums.HedgeDirection;
import com.example.fxexposure.enums.HedgeStatus;
import com.example.fxexposure.enums.HedgeType;
import com.example.fxexposure.enums.RateType;
import com.example.fxexposure.repository.HedgeAllocationRepository;
import com.example.fxexposure.repository.HedgeContractRepository;
import com.example.fxexposure.service.impl.HedgeContractServiceImpl;

@ExtendWith(MockitoExtension.class)
class HedgeContractServiceTest {

    @Mock
    private HedgeContractRepository hedgeRepository;

    @Mock
    private HedgeAllocationRepository allocationRepository;

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private AuditService auditService;

    private HedgeContractServiceImpl hedgeContractService;

    @BeforeEach
    void setUp() {
        hedgeContractService = new HedgeContractServiceImpl(hedgeRepository, allocationRepository, exchangeRateService, auditService);
        ReflectionTestUtils.setField(hedgeContractService, "defaultBaseCurrency", "USD");
    }

    @Test
    void testBookHedgeContract() {
        HedgeContractRequest req = HedgeContractRequest.builder()
                .hedgeType(HedgeType.FORWARD)
                .direction(HedgeDirection.BUY)
                .primaryCurrency("EUR")
                .secondaryCurrency("USD")
                .primaryAmount(new BigDecimal("1000000.00"))
                .strikeRate(new BigDecimal("1.080000"))
                .tradeDate(LocalDate.now())
                .valueDate(LocalDate.now().plusDays(60))
                .counterpartyBank("JPMorgan")
                .build();

        when(exchangeRateService.getExchangeRate(eq("USD"), eq("EUR"), eq(RateType.SPOT)))
                .thenReturn(new BigDecimal("1.100000"));
        when(exchangeRateService.convertAmount(eq("USD"), eq("USD"), any())).thenAnswer(i -> i.getArgument(2));

        when(hedgeRepository.save(any(HedgeContract.class))).thenAnswer(i -> {
            HedgeContract h = i.getArgument(0);
            h.setId(1L);
            return h;
        });

        HedgeContractResponse res = hedgeContractService.bookHedgeContract(req);

        assertNotNull(res);
        assertEquals("EUR", res.getPrimaryCurrency());
        assertEquals("USD", res.getSecondaryCurrency());
        assertEquals(HedgeStatus.ACTIVE, res.getStatus());
        // MtM = (1.1000 - 1.0800) * 1,000,000 = 20,000.00
        assertEquals(new BigDecimal("20000.00"), res.getCurrentMtM());
        verify(hedgeRepository).save(any(HedgeContract.class));
    }

    @Test
    void testSettleHedgeContract() {
        HedgeContract hedge = HedgeContract.builder()
                .id(1L)
                .dealReference("HDG-001")
                .hedgeType(HedgeType.FORWARD)
                .direction(HedgeDirection.SELL)
                .primaryCurrency("EUR")
                .secondaryCurrency("USD")
                .primaryAmount(new BigDecimal("500000.00"))
                .strikeRate(new BigDecimal("1.090000"))
                .status(HedgeStatus.ACTIVE)
                .counterpartyBank("Citi")
                .build();

        when(hedgeRepository.findById(1L)).thenReturn(Optional.of(hedge));
        when(hedgeRepository.save(any(HedgeContract.class))).thenAnswer(i -> i.getArgument(0));
        when(exchangeRateService.convertAmount(eq("USD"), eq("USD"), any())).thenAnswer(i -> i.getArgument(2));

        // Sell EUR at strike 1.0900. Settlement spot is 1.0700.
        // Realized gain = (1.0900 - 1.0700) * 500,000 = +10,000.00 USD
        SettlementRequest req = SettlementRequest.builder()
                .settlementRate(new BigDecimal("1.070000"))
                .build();

        HedgeContractResponse res = hedgeContractService.settleHedgeContract(1L, req);

        assertNotNull(res);
        assertEquals(HedgeStatus.SETTLED, res.getStatus());
        assertEquals(new BigDecimal("10000.00"), res.getRealizedGainLoss());
    }
}

