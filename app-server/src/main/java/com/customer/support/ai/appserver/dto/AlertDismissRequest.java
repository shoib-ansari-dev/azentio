package com.customer.support.ai.appserver.dto;

import jakarta.validation.constraints.NotBlank;

public record AlertDismissRequest(
        @NotBlank String reason) {
}
