package com.customer.support.ai.appserver.dto;

import jakarta.validation.constraints.NotNull;

public record RuleUpdateRequest(
        @NotNull Boolean enabled,
        @NotNull String parameters) {
}
