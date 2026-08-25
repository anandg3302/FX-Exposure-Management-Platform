package com.example.fxexposure.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.fxexposure.dto.ComplianceAlertResponse;
import com.example.fxexposure.dto.CurrencyExposureDto;
import com.example.fxexposure.dto.DashboardOverviewDto;
import com.example.fxexposure.dto.ExchangeRateDto;
import com.example.fxexposure.dto.MaturityBucketDto;
import com.example.fxexposure.dto.NetExposureSummaryDto;
import com.example.fxexposure.dto.ScenarioAnalysisRequest;
import com.example.fxexposure.dto.ScenarioAnalysisResultDto;
import com.example.fxexposure.dto.VaRResultDto;
import com.example.fxexposure.entity.Exposure;
import com.example.fxexposure.entity.HedgeContract;
import com.example.fxexposure.enums.AlertStatus;
import com.example.fxexposure.enums.CashFlowDirection;
import com.example.fxexposure.enums.ExposureStatus;
import com.example.fxexposure.enums.HedgeDirection;
import com.example.fxexposure.enums.HedgeStatus;
import com.example.fxexposure.enums.RateType;
import com.example.fxexposure.repository.ComplianceAlertRepository;
import com.example.fxexposure.repository.ExposureRepository;
import com.example.fxexposure.repository.HedgeContractRepository;
import com.example.fxexposure.service.AnalyticsService;
import com.example.fxexposure.service.ComplianceAlertService;
import com.example.fxexposure.service.ExchangeRateService;
import com.example.fxexposure.service.HedgeContractService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final ExposureRepository exposureRepository;
    private final HedgeContractRepository hedgeRepository;
    private final ExchangeRateService exchangeRateService;
    private final HedgeContractService hedgeContractService;
    private final ComplianceAlertRepository alertRepository;
    private final ComplianceAlertService alertService;

    @Value("${app.base-currency:USD}")
    private String defaultBaseCurrency;

    @Override
    public NetExposureSummaryDto getNetExposureSummary() {
        List<Exposure> openExposures = exposureRepository.findByStatusNot(ExposureStatus.SETTLED).stream()
                .filter(e -> e.getStatus() != ExposureStatus.CANCELLED)
                .collect(Collectors.toList());

        List<HedgeContract> activeHedges = hedgeRepository.findByStatus(HedgeStatus.ACTIVE);

        Set<String> allCurrencies = new HashSet<>();
        openExposures.forEach(e -> allCurrencies.add(e.getCurrency().toUpperCase()));
        activeHedges.forEach(h -> {
            allCurrencies.add(h.getPrimaryCurrency().toUpperCase());
            allCurrencies.add(h.getSecondaryCurrency().toUpperCase());
        });
        allCurrencies.remove(defaultBaseCurrency); // Base currency itself is not an FX risk

        List<CurrencyExposureDto> currencyBreakdowns = new ArrayList<>();
        BigDecimal totalGrossInBase = BigDecimal.ZERO;
        BigDecimal totalHedgedInBase = BigDecimal.ZERO;
        BigDecimal totalNetOpenInBase = BigDecimal.ZERO;
        BigDecimal totalPortfolioMtM = BigDecimal.ZERO;

        for (HedgeContract hedge : activeHedges) {
            if (hedge.getCurrentMtM() != null) {
                totalPortfolioMtM = totalPortfolioMtM.add(hedge.getCurrentMtM());
            }
        }

        for (String ccy : allCurrencies) {
            BigDecimal inflows = openExposures.stream()
                    .filter(e -> e.getCurrency().equalsIgnoreCase(ccy) && e.getCashFlowDirection() == CashFlowDirection.INFLOW)
                    .map(Exposure::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal outflows = openExposures.stream()
                    .filter(e -> e.getCurrency().equalsIgnoreCase(ccy) && e.getCashFlowDirection() == CashFlowDirection.OUTFLOW)
                    .map(Exposure::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal grossExp = inflows.subtract(outflows).abs();
            if (grossExp.compareTo(BigDecimal.ZERO) == 0) {
                grossExp = inflows.add(outflows);
            }

            // Hedged amount for this currency from allocations or active hedges
            BigDecimal hedged = openExposures.stream()
                    .filter(e -> e.getCurrency().equalsIgnoreCase(ccy))
                    .map(e -> e.getHedgedAmount() != null ? e.getHedgedAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal netOpenExp = grossExp.subtract(hedged).max(BigDecimal.ZERO);

            BigDecimal hedgeRatio = BigDecimal.ZERO;
            if (grossExp.compareTo(BigDecimal.ZERO) > 0) {
                hedgeRatio = hedged.divide(grossExp, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal spotRate = BigDecimal.ONE;
            BigDecimal netOpenInBase = BigDecimal.ZERO;
            BigDecimal grossInBase = BigDecimal.ZERO;
            BigDecimal hedgedInBase = BigDecimal.ZERO;

            try {
                spotRate = exchangeRateService.getExchangeRate(ccy, defaultBaseCurrency, RateType.SPOT);
                netOpenInBase = exchangeRateService.convertAmount(ccy, defaultBaseCurrency, netOpenExp);
                grossInBase = exchangeRateService.convertAmount(ccy, defaultBaseCurrency, grossExp);
                hedgedInBase = exchangeRateService.convertAmount(ccy, defaultBaseCurrency, hedged);
            } catch (Exception ex) {
                log.warn("Rate conversion failed for currency {}: {}", ccy, ex.getMessage());
            }

            totalGrossInBase = totalGrossInBase.add(grossInBase);
            totalHedgedInBase = totalHedgedInBase.add(hedgedInBase);
            totalNetOpenInBase = totalNetOpenInBase.add(netOpenInBase);

            currencyBreakdowns.add(CurrencyExposureDto.builder()
                    .currency(ccy)
                    .grossInflows(inflows)
                    .grossOutflows(outflows)
                    .grossExposure(grossExp)
                    .hedgedAmount(hedged)
                    .netOpenExposure(netOpenExp)
                    .hedgeRatioPercent(hedgeRatio)
                    .spotRateToBase(spotRate)
                    .netOpenExposureInBase(netOpenInBase)
                    .grossExposureInBase(grossInBase)
                    .hedgedAmountInBase(hedgedInBase)
                    .build());
        }

        BigDecimal overallHedgeRatio = BigDecimal.ZERO;
        if (totalGrossInBase.compareTo(BigDecimal.ZERO) > 0) {
            overallHedgeRatio = totalHedgedInBase.divide(totalGrossInBase, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
        }

        return NetExposureSummaryDto.builder()
                .baseCurrency(defaultBaseCurrency)
                .totalGrossExposureInBase(totalGrossInBase.setScale(2, RoundingMode.HALF_UP))
                .totalHedgedInBase(totalHedgedInBase.setScale(2, RoundingMode.HALF_UP))
                .totalNetOpenExposureInBase(totalNetOpenInBase.setScale(2, RoundingMode.HALF_UP))
                .overallHedgeRatioPercent(overallHedgeRatio)
                .totalPortfolioMtMInBase(totalPortfolioMtM.setScale(2, RoundingMode.HALF_UP))
                .currencyBreakdown(currencyBreakdowns)
                .build();
    }

    @Override
    public List<MaturityBucketDto> getMaturityLadder() {
        LocalDate today = LocalDate.now();
        List<Exposure> openExposures = exposureRepository.findByStatusNot(ExposureStatus.SETTLED).stream()
                .filter(e -> e.getStatus() != ExposureStatus.CANCELLED)
                .collect(Collectors.toList());

        // Define standard treasury time buckets
        List<MaturityBucketDto> buckets = new ArrayList<>();
        buckets.add(createEmptyBucket("0-30 Days", 0, 30));
        buckets.add(createEmptyBucket("31-60 Days", 31, 60));
        buckets.add(createEmptyBucket("61-90 Days", 61, 90));
        buckets.add(createEmptyBucket("91-180 Days", 91, 180));
        buckets.add(createEmptyBucket("180+ Days", 181, 3650));

        for (Exposure exp : openExposures) {
            long daysToMaturity = ChronoUnit.DAYS.between(today, exp.getValueDate());
            if (daysToMaturity < 0) {
                daysToMaturity = 0; // Overdue or maturing today goes into 0-30 bucket
            }

            for (MaturityBucketDto bucket : buckets) {
                if (daysToMaturity >= bucket.getStartDays() && daysToMaturity <= bucket.getEndDays()) {
                    BigDecimal amountInBase = exchangeRateService.convertAmount(exp.getCurrency(), defaultBaseCurrency, exp.getAmount());
                    BigDecimal hedgedInBase = exchangeRateService.convertAmount(exp.getCurrency(), defaultBaseCurrency,
                            exp.getHedgedAmount() != null ? exp.getHedgedAmount() : BigDecimal.ZERO);

                    if (exp.getCashFlowDirection() == CashFlowDirection.INFLOW) {
                        bucket.setGrossInflowsInBase(bucket.getGrossInflowsInBase().add(amountInBase));
                    } else {
                        bucket.setGrossOutflowsInBase(bucket.getGrossOutflowsInBase().add(amountInBase));
                    }

                    bucket.setHedgedAmountInBase(bucket.getHedgedAmountInBase().add(hedgedInBase));

                    // Currency breakdown in bucket
                    bucket.getCurrencyBreakdownInBase().merge(exp.getCurrency(), amountInBase, BigDecimal::add);
                    break;
                }
            }
        }

        // Finalize net amounts per bucket
        for (MaturityBucketDto bucket : buckets) {
            BigDecimal net = bucket.getGrossInflowsInBase().subtract(bucket.getGrossOutflowsInBase()).abs();
            bucket.setNetExposureInBase(net.setScale(2, RoundingMode.HALF_UP));
            bucket.setNetOpenExposureInBase(net.subtract(bucket.getHedgedAmountInBase()).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
            bucket.setGrossInflowsInBase(bucket.getGrossInflowsInBase().setScale(2, RoundingMode.HALF_UP));
            bucket.setGrossOutflowsInBase(bucket.getGrossOutflowsInBase().setScale(2, RoundingMode.HALF_UP));
            bucket.setHedgedAmountInBase(bucket.getHedgedAmountInBase().setScale(2, RoundingMode.HALF_UP));
        }

        return buckets;
    }

    private MaturityBucketDto createEmptyBucket(String name, int start, int end) {
        return MaturityBucketDto.builder()
                .bucketName(name)
                .startDays(start)
                .endDays(end)
                .grossInflowsInBase(BigDecimal.ZERO)
                .grossOutflowsInBase(BigDecimal.ZERO)
                .netExposureInBase(BigDecimal.ZERO)
                .hedgedAmountInBase(BigDecimal.ZERO)
                .netOpenExposureInBase(BigDecimal.ZERO)
                .currencyBreakdownInBase(new HashMap<>())
                .build();
    }

    @Override
    public ScenarioAnalysisResultDto runScenarioStressTest(ScenarioAnalysisRequest request) {
        NetExposureSummaryDto baselineSummary = getNetExposureSummary();
        List<HedgeContract> activeHedges = hedgeRepository.findByStatus(HedgeStatus.ACTIVE);

        Map<String, BigDecimal> currencyShocks = new HashMap<>();
        if (request != null && request.getCurrencyShocks() != null && !request.getCurrencyShocks().isEmpty()) {
            currencyShocks.putAll(request.getCurrencyShocks());
        }

        BigDecimal uniformShock = (request != null && request.getUniformPercentageShock() != null)
                ? request.getUniformPercentageShock()
                : BigDecimal.ZERO;

        BigDecimal totalExpGainLossInBase = BigDecimal.ZERO;
        BigDecimal totalHedgeGainLossInBase = BigDecimal.ZERO;
        Map<String, BigDecimal> currencyImpacts = new LinkedHashMap<>();

        for (CurrencyExposureDto ccyDto : baselineSummary.getCurrencyBreakdown()) {
            String ccy = ccyDto.getCurrency();
            BigDecimal shockPercent = currencyShocks.getOrDefault(ccy, uniformShock);

            // Shock factor: e.g. +5% shock means 0.05
            BigDecimal shockFactor = shockPercent.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);

            // Impact on unhedged exposure:
            // If currency strengthens (+shock) and we have net open exposure, value changes by (Net Open in Base) * shockFactor
            BigDecimal expImpact = ccyDto.getNetOpenExposureInBase().multiply(shockFactor).setScale(2, RoundingMode.HALF_UP);
            totalExpGainLossInBase = totalExpGainLossInBase.add(expImpact);

            // Impact on hedges for this currency:
            BigDecimal hedgeImpact = BigDecimal.ZERO;
            for (HedgeContract hedge : activeHedges) {
                if (hedge.getPrimaryCurrency().equalsIgnoreCase(ccy)) {
                    // When primary currency shifts by shockFactor:
                    BigDecimal currentRate = exchangeRateService.getExchangeRate(hedge.getSecondaryCurrency(), hedge.getPrimaryCurrency(), RateType.SPOT);
                    BigDecimal shockedRate = currentRate.multiply(BigDecimal.ONE.add(shockFactor));

                    BigDecimal deltaRate = shockedRate.subtract(currentRate);
                    BigDecimal contractDelta;
                    if (hedge.getDirection() == HedgeDirection.BUY) {
                        contractDelta = deltaRate.multiply(hedge.getPrimaryAmount());
                    } else {
                        contractDelta = deltaRate.negate().multiply(hedge.getPrimaryAmount());
                    }
                    BigDecimal contractDeltaInBase = exchangeRateService.convertAmount(hedge.getSecondaryCurrency(), defaultBaseCurrency, contractDelta);
                    hedgeImpact = hedgeImpact.add(contractDeltaInBase);
                }
            }
            totalHedgeGainLossInBase = totalHedgeGainLossInBase.add(hedgeImpact);

            BigDecimal netCcyImpact = expImpact.add(hedgeImpact).setScale(2, RoundingMode.HALF_UP);
            currencyImpacts.put(ccy, netCcyImpact);
        }

        BigDecimal netImpact = totalExpGainLossInBase.add(totalHedgeGainLossInBase).setScale(2, RoundingMode.HALF_UP);
        BigDecimal baselineVal = baselineSummary.getTotalNetOpenExposureInBase().add(baselineSummary.getTotalPortfolioMtMInBase());
        BigDecimal stressedVal = baselineVal.add(netImpact).setScale(2, RoundingMode.HALF_UP);

        BigDecimal netImpactPercent = BigDecimal.ZERO;
        if (baselineVal.compareTo(BigDecimal.ZERO) != 0) {
            netImpactPercent = netImpact.divide(baselineVal.abs(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
        }

        String summary = String.format("A scenario shock results in a Net P&L impact of %s %s (%s%%) across open exposures and hedge positions.",
                defaultBaseCurrency, netImpact, netImpactPercent);

        return ScenarioAnalysisResultDto.builder()
                .baseCurrency(defaultBaseCurrency)
                .baselinePortfolioValueInBase(baselineVal)
                .stressedPortfolioValueInBase(stressedVal)
                .exposureGainLossInBase(totalExpGainLossInBase.setScale(2, RoundingMode.HALF_UP))
                .hedgeGainLossInBase(totalHedgeGainLossInBase.setScale(2, RoundingMode.HALF_UP))
                .netPortfolioImpactInBase(netImpact)
                .netImpactPercent(netImpactPercent)
                .currencyImpactsInBase(currencyImpacts)
                .summary(summary)
                .build();
    }

    @Override
    public VaRResultDto calculateVaR() {
        NetExposureSummaryDto summary = getNetExposureSummary();
        BigDecimal netOpenExp = summary.getTotalNetOpenExposureInBase();

        // Parametric FX 1-Month VaR model:
        // Assumed average annualized currency volatility = 10.5% (approx 3.03% 1-month volatility: 10.5% / sqrt(12))
        double monthlyVol = 0.105 / Math.sqrt(12); // ~ 0.0303
        double z95 = 1.645;
        double z99 = 2.326;

        BigDecimal var95 = netOpenExp.multiply(BigDecimal.valueOf(z95 * monthlyVol)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal var99 = netOpenExp.multiply(BigDecimal.valueOf(z99 * monthlyVol)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal var95Pct = BigDecimal.ZERO;
        BigDecimal var99Pct = BigDecimal.ZERO;
        if (netOpenExp.compareTo(BigDecimal.ZERO) > 0) {
            var95Pct = var95.divide(netOpenExp, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
            var99Pct = var99.divide(netOpenExp, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
        }

        return VaRResultDto.builder()
                .baseCurrency(defaultBaseCurrency)
                .totalPortfolioNotionalInBase(netOpenExp)
                .var95Percent(var95)
                .var99Percent(var99)
                .var95PercentOfPortfolio(var95Pct)
                .var99PercentOfPortfolio(var99Pct)
                .timeHorizonDays(30)
                .methodology("Parametric Variance-Covariance (Normal Distribution, 1-Month Horizon)")
                .build();
    }

    @Override
    public DashboardOverviewDto getDashboardOverview() {
        NetExposureSummaryDto netSummary = getNetExposureSummary();
        List<MaturityBucketDto> ladder = getMaturityLadder();
        List<ExchangeRateDto> latestRates = exchangeRateService.getLatestRates();

        long openExpCount = exposureRepository.findByStatusNot(ExposureStatus.SETTLED).stream()
                .filter(e -> e.getStatus() != ExposureStatus.CANCELLED)
                .count();

        long activeHedgesCount = hedgeRepository.findByStatus(HedgeStatus.ACTIVE).size();
        long activeAlertsCount = alertRepository.countByStatus(AlertStatus.ACTIVE);

        List<ComplianceAlertResponse> criticalAlerts = alertService.getActiveAlerts().stream()
                .limit(5)
                .collect(Collectors.toList());

        return DashboardOverviewDto.builder()
                .baseCurrency(defaultBaseCurrency)
                .totalGrossExposure(netSummary.getTotalGrossExposureInBase())
                .totalHedgedAmount(netSummary.getTotalHedgedInBase())
                .totalNetOpenExposure(netSummary.getTotalNetOpenExposureInBase())
                .overallHedgeRatio(netSummary.getOverallHedgeRatioPercent())
                .portfolioMtM(netSummary.getTotalPortfolioMtMInBase())
                .totalOpenExposuresCount(openExpCount)
                .activeHedgesCount(activeHedgesCount)
                .activeAlertsCount(activeAlertsCount)
                .currencyBreakdown(netSummary.getCurrencyBreakdown())
                .maturityLadder(ladder)
                .latestRates(latestRates)
                .criticalAlerts(criticalAlerts)
                .build();
    }
}

