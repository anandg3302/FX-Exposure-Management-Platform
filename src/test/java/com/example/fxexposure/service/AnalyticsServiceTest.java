package com.example.fxexposure.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.fxexposure.dto.MaturityBucketDto;
import com.example.fxexposure.dto.NetExposureSummaryDto;
import com.example.fxexposure.dto.ScenarioAnalysisRequest;
import com.example.fxexposure.dto.ScenarioAnalysisResultDto;
import com.example.fxexposure.dto.VaRResultDto;
import com.example.fxexposure.entity.Exposure;
import com.example.fxexposure.entity.HedgeContract;
import com.example.fxexposure.enums.CashFlowDirection;
import com.example.fxexposure.enums.ExposureStatus;
import com.example.fxexposure.enums.HedgeDirection;
import com.example.fxexposure.enums.HedgeStatus;
import com.example.fxexposure.enums.HedgeType;
import com.example.fxexposure.enums.RateType;
import com.example.fxexposure.repository.ComplianceAlertRepository;
import com.example.fxexposure.repository.ExposureRepository;
import com.example.fxexposure.repository.HedgeContractRepository;
import com.example.fxexposure.service.impl.AnalyticsServiceImpl;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ExposureRepository exposureRepository;

    @Mock
    private HedgeContractRepository hedgeRepository;

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private HedgeContractService hedgeContractService;

    @Mock
    private ComplianceAlertRepository alertRepository;

    @Mock
    private ComplianceAlertService alertService;

    private AnalyticsServiceImpl analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsServiceImpl(
                exposureRepository, hedgeRepository, exchangeRateService,
                hedgeContractService, alertRepository, alertService);
        ReflectionTestUtils.setField(analyticsService, "defaultBaseCurrency", "USD");
    }

    @Test
    void testGetNetExposureSummary() {
        Exposure exp = Exposure.builder()
                .currency("EUR")
                .amount(new BigDecimal("1000000.00"))
                .hedgedAmount(new BigDecimal("800000.00"))
                .cashFlowDirection(CashFlowDirection.INFLOW)
                .status(ExposureStatus.PARTIALLY_HEDGED)
                .valueDate(LocalDate.now().plusDays(30))
                .build();

        HedgeContract hedge = HedgeContract.builder()
                .primaryCurrency("EUR")
                .secondaryCurrency("USD")
                .primaryAmount(new BigDecimal("800000.00"))
                .currentMtM(new BigDecimal("5000.00"))
                .status(HedgeStatus.ACTIVE)
                .build();

        when(exposureRepository.findByStatusNot(ExposureStatus.SETTLED)).thenReturn(List.of(exp));
        when(hedgeRepository.findByStatus(HedgeStatus.ACTIVE)).thenReturn(List.of(hedge));

        when(exchangeRateService.getExchangeRate(eq("EUR"), eq("USD"), eq(RateType.SPOT))).thenReturn(new BigDecimal("1.100000"));
        when(exchangeRateService.convertAmount(eq("EUR"), eq("USD"), eq(new BigDecimal("1000000.00")))).thenReturn(new BigDecimal("1100000.00"));
        when(exchangeRateService.convertAmount(eq("EUR"), eq("USD"), eq(new BigDecimal("800000.00")))).thenReturn(new BigDecimal("880000.00"));
        when(exchangeRateService.convertAmount(eq("EUR"), eq("USD"), eq(new BigDecimal("200000.00")))).thenReturn(new BigDecimal("220000.00"));

        NetExposureSummaryDto summary = analyticsService.getNetExposureSummary();

        assertNotNull(summary);
        assertEquals("USD", summary.getBaseCurrency());
        assertEquals(new BigDecimal("1100000.00"), summary.getTotalGrossExposureInBase());
        assertEquals(new BigDecimal("880000.00"), summary.getTotalHedgedInBase());
        assertEquals(new BigDecimal("220000.00"), summary.getTotalNetOpenExposureInBase());
        assertEquals(new BigDecimal("80.00"), summary.getOverallHedgeRatioPercent());
    }

    @Test
    void testGetMaturityLadder() {
        Exposure exp = Exposure.builder()
                .currency("EUR")
                .amount(new BigDecimal("500000.00"))
                .hedgedAmount(new BigDecimal("200000.00"))
                .cashFlowDirection(CashFlowDirection.INFLOW)
                .status(ExposureStatus.PARTIALLY_HEDGED)
                .valueDate(LocalDate.now().plusDays(20)) // within 0-30 days
                .build();

        when(exposureRepository.findByStatusNot(ExposureStatus.SETTLED)).thenReturn(List.of(exp));
        when(exchangeRateService.convertAmount(eq("EUR"), eq("USD"), eq(new BigDecimal("500000.00")))).thenReturn(new BigDecimal("550000.00"));
        when(exchangeRateService.convertAmount(eq("EUR"), eq("USD"), eq(new BigDecimal("200000.00")))).thenReturn(new BigDecimal("220000.00"));

        List<MaturityBucketDto> ladder = analyticsService.getMaturityLadder();

        assertNotNull(ladder);
        assertEquals(5, ladder.size());
        MaturityBucketDto firstBucket = ladder.get(0);
        assertEquals("0-30 Days", firstBucket.getBucketName());
        assertEquals(new BigDecimal("550000.00"), firstBucket.getGrossInflowsInBase());
        assertEquals(new BigDecimal("220000.00"), firstBucket.getHedgedAmountInBase());
        assertEquals(new BigDecimal("330000.00"), firstBucket.getNetOpenExposureInBase());
    }

    @Test
    void testScenarioStressTest() {
        Exposure exp = Exposure.builder()
                .currency("EUR")
                .amount(new BigDecimal("1000000.00"))
                .hedgedAmount(new BigDecimal("0.00"))
                .cashFlowDirection(CashFlowDirection.INFLOW)
                .status(ExposureStatus.UNHEDGED)
                .valueDate(LocalDate.now().plusDays(30))
                .build();

        when(exposureRepository.findByStatusNot(ExposureStatus.SETTLED)).thenReturn(List.of(exp));
        when(hedgeRepository.findByStatus(HedgeStatus.ACTIVE)).thenReturn(List.of());

        when(exchangeRateService.getExchangeRate(eq("EUR"), eq("USD"), eq(RateType.SPOT))).thenReturn(new BigDecimal("1.100000"));
        when(exchangeRateService.convertAmount(eq("EUR"), eq("USD"), eq(new BigDecimal("1000000.00")))).thenReturn(new BigDecimal("1100000.00"));
        when(exchangeRateService.convertAmount(eq("EUR"), eq("USD"), eq(new BigDecimal("0.00")))).thenReturn(BigDecimal.ZERO);

        // +10% shock on EUR
        ScenarioAnalysisRequest req = ScenarioAnalysisRequest.builder()
                .currencyShocks(Map.of("EUR", new BigDecimal("10.0")))
                .build();

        ScenarioAnalysisResultDto result = analyticsService.runScenarioStressTest(req);

        assertNotNull(result);
        // +10% of 1,100,000 = +110,000 USD impact
        assertEquals(new BigDecimal("110000.00"), result.getExposureGainLossInBase());
        assertEquals(new BigDecimal("110000.00"), result.getNetPortfolioImpactInBase());
    }

    @Test
    void testCalculateVaR() {
        Exposure exp = Exposure.builder()
                .currency("EUR")
                .amount(new BigDecimal("1000000.00"))
                .hedgedAmount(BigDecimal.ZERO)
                .cashFlowDirection(CashFlowDirection.INFLOW)
                .status(ExposureStatus.UNHEDGED)
                .valueDate(LocalDate.now().plusDays(30))
                .build();

        when(exposureRepository.findByStatusNot(ExposureStatus.SETTLED)).thenReturn(List.of(exp));
        when(hedgeRepository.findByStatus(HedgeStatus.ACTIVE)).thenReturn(List.of());

        when(exchangeRateService.getExchangeRate(eq("EUR"), eq("USD"), eq(RateType.SPOT))).thenReturn(new BigDecimal("1.000000"));
        when(exchangeRateService.convertAmount(eq("EUR"), eq("USD"), any())).thenReturn(new BigDecimal("1000000.00"));

        VaRResultDto varDto = analyticsService.calculateVaR();

        assertNotNull(varDto);
        assertTrue(varDto.getVar95Percent().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(varDto.getVar99Percent().compareTo(varDto.getVar95Percent()) > 0);
    }
}

