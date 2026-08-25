package com.example.fxexposure.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.fxexposure.dto.AllocationRequest;
import com.example.fxexposure.dto.ExposureRequest;
import com.example.fxexposure.dto.ExposureResponse;
import com.example.fxexposure.dto.HedgeContractRequest;
import com.example.fxexposure.dto.HedgeContractResponse;
import com.example.fxexposure.enums.CashFlowDirection;
import com.example.fxexposure.enums.ExposureType;
import com.example.fxexposure.enums.HedgeDirection;
import com.example.fxexposure.enums.HedgeType;
import com.example.fxexposure.repository.ExposureRepository;
import com.example.fxexposure.repository.HedgeContractRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializationService implements CommandLineRunner {

    private final UserService userService;
    private final ExchangeRateService exchangeRateService;
    private final RiskPolicyService riskPolicyService;
    private final ExposureService exposureService;
    private final HedgeContractService hedgeContractService;
    private final AllocationService allocationService;
    private final ExposureRepository exposureRepository;
    private final HedgeContractRepository hedgeRepository;

    @Value("${app.seed-sample-data:true}")
    private boolean seedSampleData;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing platform baseline data...");

        userService.seedDefaultUsers();
        exchangeRateService.seedDefaultRates();
        riskPolicyService.seedDefaultPolicies();

        if (seedSampleData && exposureRepository.count() == 0 && hedgeRepository.count() == 0) {
            seedSamplePortfolio();
        }

        riskPolicyService.checkCompliance();
        log.info("Platform initialization completed successfully.");
    }

    private void seedSamplePortfolio() {
        LocalDate today = LocalDate.now();

        // 1. Sample Exposures
        ExposureResponse exp1 = exposureService.createExposure(ExposureRequest.builder()
                .exposureReference("EXP-EUR-2026-001")
                .companyEntity("Acme Global Corp - Germany")
                .exposureType(ExposureType.RECEIVABLE_INVOICE)
                .cashFlowDirection(CashFlowDirection.INFLOW)
                .currency("EUR")
                .amount(new BigDecimal("1200000.00"))
                .valueDate(today.plusDays(25))
                .description("Q3 Commercial Machinery Export Invoice #8849")
                .build());

        ExposureResponse exp2 = exposureService.createExposure(ExposureRequest.builder()
                .exposureReference("EXP-GBP-2026-002")
                .companyEntity("Acme UK Operations Ltd")
                .exposureType(ExposureType.FORECASTED_REVENUE)
                .cashFlowDirection(CashFlowDirection.INFLOW)
                .currency("GBP")
                .amount(new BigDecimal("850000.00"))
                .valueDate(today.plusDays(45))
                .description("Projected British Consulting Services Billing")
                .build());

        ExposureResponse exp3 = exposureService.createExposure(ExposureRequest.builder()
                .exposureReference("EXP-JPY-2026-003")
                .companyEntity("Acme APAC Supply Chain")
                .exposureType(ExposureType.PAYABLE_INVOICE)
                .cashFlowDirection(CashFlowDirection.OUTFLOW)
                .currency("JPY")
                .amount(new BigDecimal("150000000.00"))
                .valueDate(today.plusDays(75))
                .description("Semiconductor Component Shipment Payable")
                .build());

        ExposureResponse exp4 = exposureService.createExposure(ExposureRequest.builder()
                .exposureReference("EXP-INR-2026-004")
                .companyEntity("Acme India Technology Center")
                .exposureType(ExposureType.FORECASTED_EXPENSE)
                .cashFlowDirection(CashFlowDirection.OUTFLOW)
                .currency("INR")
                .amount(new BigDecimal("95000000.00"))
                .valueDate(today.plusDays(110))
                .description("Hyderabad Tech Center Operating & Payroll Expenses")
                .build());

        ExposureResponse exp5 = exposureService.createExposure(ExposureRequest.builder()
                .exposureReference("EXP-CAD-2026-005")
                .companyEntity("Acme Canada Logistics")
                .exposureType(ExposureType.RECEIVABLE_INVOICE)
                .cashFlowDirection(CashFlowDirection.INFLOW)
                .currency("CAD")
                .amount(new BigDecimal("600000.00"))
                .valueDate(today.plusDays(15))
                .description("Toronto Distribution Center Revenue Invoice")
                .build());

        // 2. Sample Hedge Deals
        HedgeContractResponse hdg1 = hedgeContractService.bookHedgeContract(HedgeContractRequest.builder()
                .dealReference("HDG-EUR-2026-F01")
                .hedgeType(HedgeType.FORWARD)
                .direction(HedgeDirection.SELL)
                .primaryCurrency("EUR")
                .secondaryCurrency("USD")
                .primaryAmount(new BigDecimal("1000000.00"))
                .strikeRate(new BigDecimal("1.085000"))
                .tradeDate(today.minusDays(5))
                .valueDate(today.plusDays(25))
                .counterpartyBank("JPMorgan Chase")
                .build());

        HedgeContractResponse hdg2 = hedgeContractService.bookHedgeContract(HedgeContractRequest.builder()
                .dealReference("HDG-GBP-2026-F02")
                .hedgeType(HedgeType.FORWARD)
                .direction(HedgeDirection.SELL)
                .primaryCurrency("GBP")
                .secondaryCurrency("USD")
                .primaryAmount(new BigDecimal("600000.00"))
                .strikeRate(new BigDecimal("1.280000"))
                .tradeDate(today.minusDays(2))
                .valueDate(today.plusDays(45))
                .counterpartyBank("Citigroup")
                .build());

        HedgeContractResponse hdg3 = hedgeContractService.bookHedgeContract(HedgeContractRequest.builder()
                .dealReference("HDG-JPY-2026-OPT03")
                .hedgeType(HedgeType.OPTION_CALL)
                .direction(HedgeDirection.BUY)
                .primaryCurrency("JPY")
                .secondaryCurrency("USD")
                .primaryAmount(new BigDecimal("100000000.00"))
                .strikeRate(new BigDecimal("0.006500"))
                .tradeDate(today.minusDays(1))
                .valueDate(today.plusDays(75))
                .counterpartyBank("HSBC Global")
                .premiumAmount(new BigDecimal("4500.00"))
                .build());

        // 3. Link Allocations
        allocationService.allocateHedgeToExposure(AllocationRequest.builder()
                .exposureId(exp1.getId())
                .hedgeContractId(hdg1.getId())
                .allocatedAmount(new BigDecimal("1000000.00"))
                .notes("Hedged 83% of Q3 machinery invoice via JPMorgan forward contract")
                .build());

        allocationService.allocateHedgeToExposure(AllocationRequest.builder()
                .exposureId(exp2.getId())
                .hedgeContractId(hdg2.getId())
                .allocatedAmount(new BigDecimal("600000.00"))
                .notes("Hedged 70.5% of projected UK consulting billing")
                .build());

        log.info("Initialized realistic sample corporate FX portfolio with exposures, forward contracts, and allocations");
    }
}

