package com.customer.support.ai.appserver.dto;

import jakarta.validation.constraints.NotBlank;

public record CustomerUpdateRequest(
        @NotBlank String kycStatus,
        @NotBlank String riskRating) {
}
