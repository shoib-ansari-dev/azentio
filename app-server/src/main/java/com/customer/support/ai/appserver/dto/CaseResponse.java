package com.customer.support.ai.appserver.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CaseResponse(
        UUID id,
        String caseRef,
        String title,
        String status,
        String priority,
        String assignedTo,
        String notes,
        String disposition,
        String dispositionReason,
        String createdBy,
        String closedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime closedAt) {
}
