package com.turbotoggle.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turbotoggle.server.domain.entity.FlagAuditLog;
import com.turbotoggle.server.repository.FlagAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final FlagAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(FlagAuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recordMutation(String flagKey, String actionType, String performedBy, Object previousState, Object newState) {
        try {
            String prevJson = previousState != null ? objectMapper.writeValueAsString(previousState) : null;
            String newJson = newState != null ? objectMapper.writeValueAsString(newState) : null;

            FlagAuditLog log = new FlagAuditLog(flagKey, actionType, performedBy, prevJson, newJson);
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Log audit failure without blocking main thread operation
        }
    }
}