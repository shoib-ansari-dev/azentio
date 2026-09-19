package com.customer.support.ai.appserver.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JobResponse(
        UUID id,
        String jobType,
        String status,
        int totalRecords,
        int processed,
        int failed,
        String errorSummary,
        String submittedBy,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt) {
}
