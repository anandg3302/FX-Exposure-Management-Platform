package com.example.fxexposure.service;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import com.example.fxexposure.dto.ComplianceAlertResponse;
import com.example.fxexposure.dto.CurrencyExposureDto;
import com.example.fxexposure.dto.NetExposureSummaryDto;
import com.example.fxexposure.dto.RiskPolicyRequest;
import com.example.fxexposure.dto.RiskPolicyResponse;
import com.example.fxexposure.entity.RiskPolicy;
import com.example.fxexposure.enums.AlertSeverity;
import com.example.fxexposure.enums.AlertType;
import com.example.fxexposure.repository.HedgeContractRepository;
import com.example.fxexposure.repository.RiskPolicyRepository;
import com.example.fxexposure.service.impl.RiskPolicyServiceImpl;

@ExtendWith(MockitoExtension.class)
class RiskPolicyServiceTest {

    @Mock
    private RiskPolicyRepository policyRepository;

    @Mock
    private HedgeContractRepository hedgeRepository;

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private ComplianceAlertService alertService;

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private AuditService auditService;

    private RiskPolicyServiceImpl policyService;

    @BeforeEach
    void setUp() {
        policyService = new RiskPolicyServiceImpl(
                policyRepository, hedgeRepository, analyticsService,
                alertService, exchangeRateService, auditService);
    }

    @Test
    void testCreatePolicy() {
        RiskPolicyRequest req = RiskPolicyRequest.builder()
                .policyName("Test Policy")
                .currency("EUR")
                .maxUnhedgedExposure(new BigDecimal("1000000.00"))
                .minHedgeRatio(new BigDecimal("70.00"))
                .maxCounterpartyExposure(new BigDecimal("5000000.00"))
                .active(true)
                .build();

        when(policyRepository.save(any(RiskPolicy.class))).thenAnswer(i -> {
            RiskPolicy p = i.getArgument(0);
            p.setId(1L);
            return p;
        });

        RiskPolicyResponse res = policyService.createPolicy(req);

        assertNotNull(res);
        assertEquals("Test Policy", res.getPolicyName());
        assertEquals("EUR", res.getCurrency());
        verify(policyRepository).save(any(RiskPolicy.class));
    }

    @Test
    void testCheckComplianceGeneratesAlertOnLimitBreach() {
        RiskPolicy policy = RiskPolicy.builder()
                .id(1L)
                .policyName("EUR Policy")
                .currency("EUR")
                .maxUnhedgedExposure(new BigDecimal("500000.00"))
                .minHedgeRatio(new BigDecimal("70.00"))
                .maxCounterpartyExposure(new BigDecimal("5000000.00"))
                .warningThresholdPercent(new BigDecimal("80.00"))
                .active(true)
                .build();

        CurrencyExposureDto ccyDto = CurrencyExposureDto.builder()
                .currency("EUR")
                .grossExposure(new BigDecimal("1000000.00"))
                .hedgedAmount(new BigDecimal("200000.00"))
                .netOpenExposure(new BigDecimal("800000.00"))
                .netOpenExposureInBase(new BigDecimal("880000.00")) // Exceeds maxUnhedgedExposure 500,000
                .hedgeRatioPercent(new BigDecimal("20.00")) // Below min 70%
                .build();

        NetExposureSummaryDto summary = NetExposureSummaryDto.builder()
                .currencyBreakdown(List.of(ccyDto))
                .build();

        when(policyRepository.findByActiveTrue()).thenReturn(List.of(policy));
        when(analyticsService.getNetExposureSummary()).thenReturn(summary);
        when(hedgeRepository.findByStatus(any())).thenReturn(List.of());
        when(hedgeRepository.findByValueDateBetween(any(), any())).thenReturn(List.of());

        ComplianceAlertResponse mockAlert = ComplianceAlertResponse.builder()
                .id(10L)
                .alertType(AlertType.UNHEDGED_LIMIT_BREACH)
                .severity(AlertSeverity.CRITICAL)
                .message("Limit breached")
                .build();

        when(alertService.createAlert(eq(AlertType.UNHEDGED_LIMIT_BREACH), eq(AlertSeverity.CRITICAL), any(), any(), any(), any(), any(), any()))
                .thenReturn(mockAlert);

        List<ComplianceAlertResponse> alerts = policyService.checkCompliance();

        assertNotNull(alerts);
        assertFalse(alerts.isEmpty());
        assertEquals(AlertType.UNHEDGED_LIMIT_BREACH, alerts.get(0).getAlertType());
    }
}

