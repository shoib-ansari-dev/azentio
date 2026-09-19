package com.customer.support.ai.appserver.dto;

import jakarta.validation.constraints.NotBlank;

public record AccountUpdateRequest(
        @NotBlank String accountStatus,
        @NotBlank String riskRating) {
}
