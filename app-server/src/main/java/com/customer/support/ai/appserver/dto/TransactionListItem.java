package com.customer.support.ai.appserver.dto;

import com.customer.support.ai.appserver.entity.Transaction;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionListItem(
        UUID id,
        String transactionRef,
        BigDecimal amount,
        String currency,
        BigDecimal amountInr,
        String transactionType,
        OffsetDateTime transactionTimestamp,
        String counterpartyAccount,
        String counterpartyBank,
        String counterpartyJurisdiction,
        String status) {

    public static TransactionListItem from(Transaction t) {
        return new TransactionListItem(
                t.getId(),
                t.getTransactionRef(),
                t.getAmount(),
                t.getCurrency(),
                t.getAmountInr(),
                t.getTransactionType(),
                t.getTransactionTimestamp(),
                t.getCounterpartyAccount(),
                t.getCounterpartyBank(),
                t.getCounterpartyJurisdiction(),
                t.getStatus());
    }
}
