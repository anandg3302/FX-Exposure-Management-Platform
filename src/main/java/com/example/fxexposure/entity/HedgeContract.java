package com.example.fxexposure.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.fxexposure.enums.HedgeDirection;
import com.example.fxexposure.enums.HedgeStatus;
import com.example.fxexposure.enums.HedgeType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "hedge_contracts", indexes = {
        @Index(name = "idx_hdg_ref", columnList = "dealReference", unique = true),
        @Index(name = "idx_hdg_pair", columnList = "primaryCurrency, secondaryCurrency"),
        @Index(name = "idx_hdg_status", columnList = "status"),
        @Index(name = "idx_hdg_value_date", columnList = "valueDate"),
        @Index(name = "idx_hdg_counterparty", columnList = "counterpartyBank")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class HedgeContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String dealReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HedgeType hedgeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private HedgeDirection direction;

    @Column(nullable = false, length = 3)
    private String primaryCurrency;

    @Column(nullable = false, length = 3)
    private String secondaryCurrency;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal primaryAmount;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal secondaryAmount;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal strikeRate;

    @Column(nullable = false)
    private LocalDate tradeDate;

    @Column(nullable = false)
    private LocalDate valueDate;

    @Column(nullable = false, length = 100)
    private String counterpartyBank;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private HedgeStatus status = HedgeStatus.ACTIVE;

    @Column(precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal premiumAmount = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal currentMtM = BigDecimal.ZERO;

    @Column(precision = 18, scale = 6)
    private BigDecimal settledRate;

    @Column(precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal realizedGainLoss = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal allocatedAmount = BigDecimal.ZERO;

    @Column(length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;
}

