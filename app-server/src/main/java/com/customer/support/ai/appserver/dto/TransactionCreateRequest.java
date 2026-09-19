package com.customer.support.ai.appserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionCreateRequest(
        @NotBlank String transactionRef,
        @NotNull UUID accountId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String currency,
        String transactionType,
        String channel,
        String counterpartyAccount,
        String counterpartyBank,
        String counterpartyJurisdiction,
        String description,
        @NotNull OffsetDateTime transactionTimestamp) {
}
