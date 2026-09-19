package com.customer.support.ai.appserver.service;

import com.customer.support.ai.appserver.entity.AuditLog;
import com.customer.support.ai.appserver.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // fire-and-forget audit write on the audit pool
    @Async("auditExecutor")
    public void log(String entityType, UUID entityId, String action, String actor,
            String oldValue, String newValue) {
        AuditLog entry = new AuditLog();
        entry.setId(UUID.randomUUID());
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setActor(actor);
        entry.setOldValue(toJson(oldValue));
        entry.setNewValue(toJson(newValue));
        entry.setTimestamp(OffsetDateTime.now());
        auditLogRepository.save(entry);
    }

    // wrap raw value as a valid JSON document for the jsonb column
    private String toJson(String value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize audit value", e);
        }
    }
}
