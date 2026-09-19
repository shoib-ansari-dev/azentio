package com.customer.support.ai.appserver.dto;

public record CaseUpdateRequest(
        String assignedTo,
        String notes) {
}
