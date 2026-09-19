package com.customer.support.ai.appserver.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RuleResponse(
        UUID id,
        String ruleCode,
        String name,
        String description,
        boolean enabled,
        int riskWeight,
        String parameters,
        int version,
        String updatedBy,
        OffsetDateTime updatedAt) {
}
