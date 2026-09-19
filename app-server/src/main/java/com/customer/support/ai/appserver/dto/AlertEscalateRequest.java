package com.customer.support.ai.appserver.dto;

import java.util.UUID;

public record AlertEscalateRequest(
        UUID caseId) {
}
