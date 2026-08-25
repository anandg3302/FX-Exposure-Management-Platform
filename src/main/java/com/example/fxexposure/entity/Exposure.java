package com.example.fxexposure.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.fxexposure.enums.CashFlowDirection;
import com.example.fxexposure.enums.ExposureStatus;
import com.example.fxexposure.enums.ExposureType;

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
@Table(name = "exposures", indexes = {
        @Index(name = "idx_exp_ref", columnList = "exposureReference", unique = true),
        @Index(name = "idx_exp_currency", columnList = "currency"),
        @Index(name = "idx_exp_status", columnList = "status"),
        @Index(name = "idx_exp_value_date", columnList = "valueDate")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Exposure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String exposureReference;

    @Column(nullable = false, length = 100)
    private String companyEntity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExposureType exposureType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CashFlowDirection cashFlowDirection;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String baseCurrency = "USD";

    @Column(nullable = false)
    private LocalDate valueDate;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ExposureStatus status = ExposureStatus.UNHEDGED;

    @Column(nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal hedgedAmount = BigDecimal.ZERO;

    @Column(length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;
}

