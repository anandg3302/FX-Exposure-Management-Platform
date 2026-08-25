package com.example.fxexposure.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "risk_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class RiskPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String policyName;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "ALL";

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal maxUnhedgedExposure;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal minHedgeRatio = new BigDecimal("70.00");

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal maxCounterpartyExposure;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal warningThresholdPercent = new BigDecimal("80.00");

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(length = 255)
    private String description;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;
}

