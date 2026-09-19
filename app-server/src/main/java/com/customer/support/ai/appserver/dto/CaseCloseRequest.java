package com.customer.support.ai.appserver.dto;

import jakarta.validation.constraints.NotBlank;

public record CaseCloseRequest(
        @NotBlank String disposition,
        @NotBlank String dispositionReason) {
}
