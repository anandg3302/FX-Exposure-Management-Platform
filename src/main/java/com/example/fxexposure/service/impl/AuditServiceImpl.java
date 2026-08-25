package com.example.fxexposure.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fxexposure.entity.AuditLog;
import com.example.fxexposure.repository.AuditLogRepository;
import com.example.fxexposure.security.SecurityUtils;
import com.example.fxexposure.service.AuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void log(String action, String entityName, Long entityId, String performedBy, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityName(entityName)
                    .entityId(entityId)
                    .performedBy(performedBy != null ? performedBy : "system")
                    .details(details)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.error("Failed to write audit log: {}", ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void log(String action, String entityName, Long entityId, String details) {
        String currentUser = SecurityUtils.getCurrentUserEmail();
        log(action, entityName, entityId, currentUser, details);
    }

    @Override
    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc();
    }

    @Override
    public List<AuditLog> getLogsForEntity(String entityName, Long entityId) {
        return auditLogRepository.findByEntityNameAndEntityIdOrderByTimestampDesc(entityName, entityId);
    }
}

