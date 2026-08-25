package com.example.fxexposure.service;

import java.util.List;

import com.example.fxexposure.entity.AuditLog;

public interface AuditService {

    void log(String action, String entityName, Long entityId, String performedBy, String details);

    void log(String action, String entityName, Long entityId, String details);

    List<AuditLog> getRecentLogs();

    List<AuditLog> getLogsForEntity(String entityName, Long entityId);
}

