package com.customer.support.ai.appserver.dto;

import com.customer.support.ai.appserver.entity.Transaction;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionDetailResponse(
        UUID id,
        String transactionRef,
        UUID accountId,
        BigDecimal amount,
        String currency,
        BigDecimal amountInr,
        BigDecimal exchangeRateUsed,
        String transactionType,
        String channel,
        String counterpartyAccount,
        String counterpartyBank,
        String counterpartyJurisdiction,
        String description,
        OffsetDateTime transactionTimestamp,
        OffsetDateTime ingestedAt,
        String status,
        UUID jobId) {

    public static TransactionDetailResponse from(Transaction t) {
        return new TransactionDetailResponse(
                t.getId(),
                t.getTransactionRef(),
                t.getAccountId(),
                t.getAmount(),
                t.getCurrency(),
                t.getAmountInr(),
                t.getExchangeRateUsed(),
                t.getTransactionType(),
                t.getChannel(),
                t.getCounterpartyAccount(),
                t.getCounterpartyBank(),
                t.getCounterpartyJurisdiction(),
                t.getDescription(),
                t.getTransactionTimestamp(),
                t.getIngestedAt(),
                t.getStatus(),
                t.getJobId());
    }
}
