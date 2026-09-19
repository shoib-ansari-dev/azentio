package com.customer.support.ai.appserver.detection.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.customer.support.ai.appserver.detection.RuleContext;
import com.customer.support.ai.appserver.detection.RuleHit;
import com.customer.support.ai.appserver.entity.DetectionRule;
import com.customer.support.ai.appserver.entity.Transaction;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BehavioralDeviationEvaluatorTest {

    private final BehavioralDeviationEvaluator evaluator = new BehavioralDeviationEvaluator();
    private final RuleContext.TransactionHistory history = mock(RuleContext.TransactionHistory.class);

    private final UUID accountId = UUID.randomUUID();

    private DetectionRule rule(int riskWeight, int lookbackDays, String multiplier) {
        DetectionRule rule = new DetectionRule();
        rule.setRuleCode("BEHAVIORAL_DEVIATION");
        rule.setRiskWeight(riskWeight);
        rule.setParameters(
                "{\"lookback_days\": " + lookbackDays + ", \"multiplier\": " + multiplier + "}");
        return rule;
    }

    private Transaction txn(BigDecimal amountInr) {
        Transaction txn = new Transaction();
        txn.setId(UUID.randomUUID());
        txn.setAccountId(accountId);
        txn.setAmountInr(amountInr);
        txn.setTransactionTimestamp(OffsetDateTime.parse("2024-01-31T12:00:00Z"));
        return txn;
    }

    private Transaction baseline(BigDecimal amountInr) {
        Transaction t = new Transaction();
        t.setId(UUID.randomUUID());
        t.setAccountId(accountId);
        t.setAmountInr(amountInr);
        t.setTransactionTimestamp(OffsetDateTime.parse("2024-01-15T12:00:00Z"));
        return t;
    }

    @Test
    void ruleCodeMatches() {
        assertThat(evaluator.ruleCode()).isEqualTo("BEHAVIORAL_DEVIATION");
    }

    @Test
    void firesWhenAmountExceedsMultiplierOfAverage() {
        // baseline avg = 100, multiplier 3 -> threshold 300; trigger 301 fires
        Transaction trigger = txn(new BigDecimal("301"));
        Transaction b1 = baseline(new BigDecimal("100"));
        Transaction b2 = baseline(new BigDecimal("100"));
        when(history.byAccountBetween(eq(accountId), any(), any()))
                .thenReturn(List.of(trigger, b1, b2));
        DetectionRule rule = rule(60, 90, "3");
        RuleContext ctx = new RuleContext(trigger, rule, history);

        Optional<RuleHit> result = evaluator.evaluate(ctx);

        assertThat(result).isPresent();
        RuleHit hit = result.get();
        assertThat(hit.ruleCode()).isEqualTo("BEHAVIORAL_DEVIATION");
        assertThat(hit.riskWeight()).isEqualTo(rule.getRiskWeight());
        assertThat(hit.evidenceTxnIds()).containsExactly(trigger.getId());
        assertThat(hit.explanation()).isNotBlank();
    }

    @Test
    void excludesTriggeringTxnFromBaselineAverage() {
        // if trigger were included, avg would rise and it would NOT fire.
        // baseline (excluding trigger) avg = 100 -> threshold 300; trigger 301 fires.
        Transaction trigger = txn(new BigDecimal("301"));
        Transaction b1 = baseline(new BigDecimal("100"));
        Transaction b2 = baseline(new BigDecimal("100"));
        when(history.byAccountBetween(eq(accountId), any(), any()))
                .thenReturn(List.of(trigger, b1, b2));
        RuleContext ctx = new RuleContext(trigger, rule(60, 90, "3"), history);

        assertThat(evaluator.evaluate(ctx)).isPresent();
    }

    @Test
    void doesNotFireWhenExactlyAtMultiplier() {
        // avg 100, multiplier 3 -> threshold 300; amount 300 is NOT > threshold
        Transaction trigger = txn(new BigDecimal("300"));
        Transaction b1 = baseline(new BigDecimal("100"));
        Transaction b2 = baseline(new BigDecimal("100"));
        when(history.byAccountBetween(eq(accountId), any(), any()))
                .thenReturn(List.of(trigger, b1, b2));
        RuleContext ctx = new RuleContext(trigger, rule(60, 90, "3"), history);

        assertThat(evaluator.evaluate(ctx)).isEmpty();
    }

    @Test
    void doesNotFireWhenJustUnderMultiplier() {
        // avg 100, threshold 300; amount 299 is under
        Transaction trigger = txn(new BigDecimal("299"));
        Transaction b1 = baseline(new BigDecimal("100"));
        Transaction b2 = baseline(new BigDecimal("100"));
        when(history.byAccountBetween(eq(accountId), any(), any()))
                .thenReturn(List.of(trigger, b1, b2));
        RuleContext ctx = new RuleContext(trigger, rule(60, 90, "3"), history);

        assertThat(evaluator.evaluate(ctx)).isEmpty();
    }

    @Test
    void doesNotFireWhenHistoryEmpty() {
        Transaction trigger = txn(new BigDecimal("10000"));
        when(history.byAccountBetween(eq(accountId), any(), any())).thenReturn(List.of());
        RuleContext ctx = new RuleContext(trigger, rule(60, 90, "3"), history);

        assertThat(evaluator.evaluate(ctx)).isEmpty();
    }

    @Test
    void doesNotFireWhenOnlyTriggerInWindow() {
        // trigger is excluded, leaving count 0 -> empty
        Transaction trigger = txn(new BigDecimal("10000"));
        when(history.byAccountBetween(eq(accountId), any(), any())).thenReturn(List.of(trigger));
        RuleContext ctx = new RuleContext(trigger, rule(60, 90, "3"), history);

        assertThat(evaluator.evaluate(ctx)).isEmpty();
    }

    @Test
    void doesNotFireWhenTriggerAmountNull() {
        Transaction trigger = txn(null);
        RuleContext ctx = new RuleContext(trigger, rule(60, 90, "3"), history);

        assertThat(evaluator.evaluate(ctx)).isEmpty();
    }
}
