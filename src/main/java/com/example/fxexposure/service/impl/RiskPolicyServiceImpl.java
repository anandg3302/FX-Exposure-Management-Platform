package com.example.fxexposure.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fxexposure.dto.ComplianceAlertResponse;
import com.example.fxexposure.dto.CurrencyExposureDto;
import com.example.fxexposure.dto.NetExposureSummaryDto;
import com.example.fxexposure.dto.RiskPolicyRequest;
import com.example.fxexposure.dto.RiskPolicyResponse;
import com.example.fxexposure.entity.HedgeContract;
import com.example.fxexposure.entity.RiskPolicy;
import com.example.fxexposure.enums.AlertSeverity;
import com.example.fxexposure.enums.AlertType;
import com.example.fxexposure.enums.HedgeStatus;
import com.example.fxexposure.exception.ResourceNotFoundException;
import com.example.fxexposure.repository.HedgeContractRepository;
import com.example.fxexposure.repository.RiskPolicyRepository;
import com.example.fxexposure.service.AnalyticsService;
import com.example.fxexposure.service.AuditService;
import com.example.fxexposure.service.ComplianceAlertService;
import com.example.fxexposure.service.ExchangeRateService;
import com.example.fxexposure.service.RiskPolicyService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RiskPolicyServiceImpl implements RiskPolicyService {

    private final RiskPolicyRepository policyRepository;
    private final HedgeContractRepository hedgeRepository;
    private final AnalyticsService analyticsService;
    private final ComplianceAlertService alertService;
    private final ExchangeRateService exchangeRateService;
    private final AuditService auditService;

    public RiskPolicyServiceImpl(
            RiskPolicyRepository policyRepository,
            HedgeContractRepository hedgeRepository,
            @Lazy AnalyticsService analyticsService,
            ComplianceAlertService alertService,
            ExchangeRateService exchangeRateService,
            AuditService auditService) {
        this.policyRepository = policyRepository;
        this.hedgeRepository = hedgeRepository;
        this.analyticsService = analyticsService;
        this.alertService = alertService;
        this.exchangeRateService = exchangeRateService;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public RiskPolicyResponse createPolicy(RiskPolicyRequest request) {
        RiskPolicy policy = RiskPolicy.builder()
                .policyName(request.getPolicyName())
                .currency(request.getCurrency() != null ? request.getCurrency().toUpperCase().trim() : "ALL")
                .maxUnhedgedExposure(request.getMaxUnhedgedExposure())
                .minHedgeRatio(request.getMinHedgeRatio())
                .maxCounterpartyExposure(request.getMaxCounterpartyExposure())
                .warningThresholdPercent(request.getWarningThresholdPercent() != null ? request.getWarningThresholdPercent() : new BigDecimal("80.00"))
                .active(request.isActive())
                .description(request.getDescription())
                .build();

        RiskPolicy saved = policyRepository.save(policy);
        auditService.log("CREATE_POLICY", "RiskPolicy", saved.getId(), "Created risk policy: " + saved.getPolicyName());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public RiskPolicyResponse updatePolicy(Long id, RiskPolicyRequest request) {
        RiskPolicy policy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RiskPolicy", "id", id));

        policy.setPolicyName(request.getPolicyName());
        policy.setCurrency(request.getCurrency() != null ? request.getCurrency().toUpperCase().trim() : "ALL");
        policy.setMaxUnhedgedExposure(request.getMaxUnhedgedExposure());
        policy.setMinHedgeRatio(request.getMinHedgeRatio());
        policy.setMaxCounterpartyExposure(request.getMaxCounterpartyExposure());
        if (request.getWarningThresholdPercent() != null) {
            policy.setWarningThresholdPercent(request.getWarningThresholdPercent());
        }
        policy.setActive(request.isActive());
        policy.setDescription(request.getDescription());

        RiskPolicy updated = policyRepository.save(policy);
        auditService.log("UPDATE_POLICY", "RiskPolicy", updated.getId(), "Updated risk policy: " + updated.getPolicyName());
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deletePolicy(Long id) {
        RiskPolicy policy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RiskPolicy", "id", id));
        policyRepository.delete(policy);
        auditService.log("DELETE_POLICY", "RiskPolicy", id, "Deleted risk policy: " + policy.getPolicyName());
    }

    @Override
    public List<RiskPolicyResponse> getActivePolicies() {
        return policyRepository.findByActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<RiskPolicyResponse> getAllPolicies() {
        return policyRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RiskPolicyResponse getPolicyById(Long id) {
        RiskPolicy policy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RiskPolicy", "id", id));
        return mapToResponse(policy);
    }

    @Override
    @Transactional
    public List<ComplianceAlertResponse> checkCompliance() {
        List<RiskPolicy> activePolicies = policyRepository.findByActiveTrue();
        NetExposureSummaryDto summary = analyticsService.getNetExposureSummary();
        List<ComplianceAlertResponse> generatedAlerts = new ArrayList<>();

        for (RiskPolicy policy : activePolicies) {
            String policyCurrency = policy.getCurrency();

            List<CurrencyExposureDto> targetDtos = summary.getCurrencyBreakdown().stream()
                    .filter(c -> "ALL".equalsIgnoreCase(policyCurrency) || c.getCurrency().equalsIgnoreCase(policyCurrency))
                    .collect(Collectors.toList());

            for (CurrencyExposureDto ccyDto : targetDtos) {
                String ccy = ccyDto.getCurrency();
                BigDecimal netOpenInBase = ccyDto.getNetOpenExposureInBase();
                BigDecimal maxLimit = policy.getMaxUnhedgedExposure();
                BigDecimal warningRatio = policy.getWarningThresholdPercent().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                BigDecimal warningLimit = maxLimit.multiply(warningRatio);

                // 1. Check Max Unhedged Exposure Limit
                if (netOpenInBase.compareTo(maxLimit) > 0) {
                    ComplianceAlertResponse alert = alertService.createAlert(
                            AlertType.UNHEDGED_LIMIT_BREACH,
                            AlertSeverity.CRITICAL,
                            String.format("Unhedged %s exposure (%s USD) exceeds policy limit (%s USD)", ccy, netOpenInBase, maxLimit),
                            ccy, "RiskPolicy", policy.getId(), netOpenInBase, maxLimit
                    );
                    if (alert != null) generatedAlerts.add(alert);
                } else if (netOpenInBase.compareTo(warningLimit) > 0) {
                    ComplianceAlertResponse alert = alertService.createAlert(
                            AlertType.UNHEDGED_LIMIT_BREACH,
                            AlertSeverity.WARNING,
                            String.format("Unhedged %s exposure (%s USD) is approaching policy limit (%s USD)", ccy, netOpenInBase, maxLimit),
                            ccy, "RiskPolicy", policy.getId(), netOpenInBase, warningLimit
                    );
                    if (alert != null) generatedAlerts.add(alert);
                }

                // 2. Check Minimum Hedge Ratio
                BigDecimal actualHedgeRatio = ccyDto.getHedgeRatioPercent();
                if (ccyDto.getGrossExposure().compareTo(BigDecimal.ZERO) > 0 && actualHedgeRatio.compareTo(policy.getMinHedgeRatio()) < 0) {
                    ComplianceAlertResponse alert = alertService.createAlert(
                            AlertType.HEDGE_RATIO_LOW,
                            AlertSeverity.WARNING,
                            String.format("Hedge ratio for %s (%s%%) is below mandated policy minimum (%s%%)", ccy, actualHedgeRatio, policy.getMinHedgeRatio()),
                            ccy, "RiskPolicy", policy.getId(), actualHedgeRatio, policy.getMinHedgeRatio()
                    );
                    if (alert != null) generatedAlerts.add(alert);
                }
            }

            // 3. Check Counterparty Bank Concentration Limits
            List<HedgeContract> activeHedges = hedgeRepository.findByStatus(HedgeStatus.ACTIVE);
            Map<String, List<HedgeContract>> hedgesByBank = activeHedges.stream()
                    .collect(Collectors.groupingBy(HedgeContract::getCounterpartyBank));

            for (Map.Entry<String, List<HedgeContract>> entry : hedgesByBank.entrySet()) {
                String bank = entry.getKey();
                BigDecimal bankTotalInBase = BigDecimal.ZERO;
                for (HedgeContract h : entry.getValue()) {
                    bankTotalInBase = bankTotalInBase.add(exchangeRateService.convertAmount(h.getSecondaryCurrency(), "USD", h.getSecondaryAmount()));
                }

                if (bankTotalInBase.compareTo(policy.getMaxCounterpartyExposure()) > 0) {
                    ComplianceAlertResponse alert = alertService.createAlert(
                            AlertType.COUNTERPARTY_LIMIT_EXCEEDED,
                            AlertSeverity.CRITICAL,
                            String.format("Counterparty exposure to %s (%s USD) exceeds limit (%s USD)", bank, bankTotalInBase, policy.getMaxCounterpartyExposure()),
                            null, "CounterpartyBank", null, bankTotalInBase, policy.getMaxCounterpartyExposure()
                    );
                    if (alert != null) generatedAlerts.add(alert);
                }
            }
        }

        // 4. Check for Upcoming Maturing Contracts (next 7 days)
        LocalDate sevenDaysOut = LocalDate.now().plusDays(7);
        List<HedgeContract> maturingSoon = hedgeRepository.findByValueDateBetween(LocalDate.now(), sevenDaysOut).stream()
                .filter(h -> h.getStatus() == HedgeStatus.ACTIVE)
                .collect(Collectors.toList());

        for (HedgeContract h : maturingSoon) {
            ComplianceAlertResponse alert = alertService.createAlert(
                    AlertType.UPCOMING_MATURITY,
                    AlertSeverity.INFO,
                    String.format("Hedge contract %s (%s %s) matures in next 7 days on %s",
                            h.getDealReference(), h.getPrimaryAmount(), h.getPrimaryCurrency(), h.getValueDate()),
                    h.getPrimaryCurrency(), "HedgeContract", h.getId(), h.getPrimaryAmount(), BigDecimal.ZERO
            );
            if (alert != null) generatedAlerts.add(alert);
        }

        return generatedAlerts;
    }

    @Override
    @Transactional
    public void seedDefaultPolicies() {
        if (policyRepository.count() > 0) {
            return;
        }

        policyRepository.save(RiskPolicy.builder()
                .policyName("Global Treasury FX Risk Policy")
                .currency("ALL")
                .maxUnhedgedExposure(new BigDecimal("2000000.00")) // 2M USD limit per currency
                .minHedgeRatio(new BigDecimal("70.00")) // 70% min coverage
                .maxCounterpartyExposure(new BigDecimal("10000000.00")) // 10M USD max counterparty
                .warningThresholdPercent(new BigDecimal("80.00"))
                .active(true)
                .description("Default enterprise risk policy establishing max open exposure and min hedge coverage ratio.")
                .build());

        policyRepository.save(RiskPolicy.builder()
                .policyName("EUR Specfic Exposure Policy")
                .currency("EUR")
                .maxUnhedgedExposure(new BigDecimal("1500000.00"))
                .minHedgeRatio(new BigDecimal("75.00"))
                .maxCounterpartyExposure(new BigDecimal("8000000.00"))
                .warningThresholdPercent(new BigDecimal("80.00"))
                .active(true)
                .description("Specific tighter limits for primary European operations.")
                .build());

        log.info("Initialized default treasury risk policies");
    }

    private RiskPolicyResponse mapToResponse(RiskPolicy policy) {
        return RiskPolicyResponse.builder()
                .id(policy.getId())
                .policyName(policy.getPolicyName())
                .currency(policy.getCurrency())
                .maxUnhedgedExposure(policy.getMaxUnhedgedExposure())
                .minHedgeRatio(policy.getMinHedgeRatio())
                .maxCounterpartyExposure(policy.getMaxCounterpartyExposure())
                .warningThresholdPercent(policy.getWarningThresholdPercent())
                .active(policy.isActive())
                .description(policy.getDescription())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}

