package com.example.fxexposure.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "hedge_allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"exposure", "hedgeContract"})
public class HedgeAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exposure_id", nullable = false)
    private Exposure exposure;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hedge_contract_id", nullable = false)
    private HedgeContract hedgeContract;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal allocatedAmount;

    @Column(precision = 18, scale = 6)
    private BigDecimal effectiveRate;

    @Column(nullable = false)
    private LocalDate allocationDate;

    @Column(length = 255)
    private String notes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;
}

