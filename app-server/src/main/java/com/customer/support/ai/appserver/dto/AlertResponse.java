package com.customer.support.ai.appserver.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        String alertRef,
        UUID accountId,
        UUID customerId,
        String ruleCode,
        String status,
        int riskScore,
        String explanation,
        List<UUID> evidenceTxnIds,
        String dispositionReason,
        String assignedTo,
        UUID caseId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String customerMasked,
        String accountRef) {
}
