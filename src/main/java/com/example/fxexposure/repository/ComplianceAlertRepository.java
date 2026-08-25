package com.example.fxexposure.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fxexposure.entity.ComplianceAlert;
import com.example.fxexposure.enums.AlertSeverity;
import com.example.fxexposure.enums.AlertStatus;
import com.example.fxexposure.enums.AlertType;

@Repository
public interface ComplianceAlertRepository extends JpaRepository<ComplianceAlert, Long> {

    List<ComplianceAlert> findByStatusOrderByCreatedAtDesc(AlertStatus status);

    List<ComplianceAlert> findByStatusAndSeverityOrderByCreatedAtDesc(AlertStatus status, AlertSeverity severity);

    long countByStatus(AlertStatus status);

    boolean existsByAlertTypeAndCurrencyAndStatus(AlertType alertType, String currency, AlertStatus status);
}

