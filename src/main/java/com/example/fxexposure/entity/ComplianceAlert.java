package com.example.fxexposure.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.fxexposure.enums.AlertSeverity;
import com.example.fxexposure.enums.AlertStatus;
import com.example.fxexposure.enums.AlertType;

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
@Table(name = "compliance_alerts", indexes = {
        @Index(name = "idx_alert_status", columnList = "status"),
        @Index(name = "idx_alert_severity", columnList = "severity"),
        @Index(name = "idx_alert_type", columnList = "alertType")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ComplianceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private AlertSeverity severity;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(length = 10)
    private String currency;

    @Column(length = 50)
    private String relatedEntity;

    private Long relatedEntityId;

    @Column(precision = 18, scale = 2)
    private BigDecimal currentValue;

    @Column(precision = 18, scale = 2)
    private BigDecimal thresholdValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AlertStatus status = AlertStatus.ACTIVE;

    @Column(length = 100)
    private String acknowledgedBy;

    private LocalDateTime acknowledgedAt;

    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column
    private LocalDateTime updatedAt;
}

