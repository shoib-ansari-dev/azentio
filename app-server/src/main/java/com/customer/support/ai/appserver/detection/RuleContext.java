package com.customer.support.ai.appserver.detection;

import com.customer.support.ai.appserver.entity.DetectionRule;
import com.customer.support.ai.appserver.entity.Transaction;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RuleContext(
        Transaction transaction,
        DetectionRule rule,
        TransactionHistory history) {

    public interface TransactionHistory {
        List<Transaction> byAccountBetween(UUID accountId, OffsetDateTime from, OffsetDateTime to);
    }
}
