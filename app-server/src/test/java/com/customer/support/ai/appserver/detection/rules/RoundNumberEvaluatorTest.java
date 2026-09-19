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

class RoundNumberEvaluatorTest {

    private final RoundNumberEvaluator evaluator = new RoundNumberEvaluator();
    private final RuleContext.TransactionHistory history = mock(RuleContext.TransactionHistory.class);

    private final UUID accountId = UUID.randomUUID();

    private DetectionRule rule(int riskWeight, int windowHours, int minCount) {
        DetectionRule rule = new DetectionRule();
        rule.setRuleCode("ROUND_NUMBER");
        rule.setRiskWeight(riskWeight);
        rule.setParameters(
                "{\"window_hours\": " + windowHours + ", \"min_count\": " + minCount + "}");
        return rule;
    }

    private Transaction txn(BigDecimal amountInr) {
        Transaction t = new Transaction();
        t.setId(UUID.randomUUID());
        t.setAccountId(accountId);
        t.setAmountInr(amountInr);
        t.setTransactionTimestamp(OffsetDateTime.parse("2024-01-31T12:00:00Z"));
        return t;
    }

    private RuleContext contextWith(DetectionRule rule, List<Transaction> window) {
        Transaction trigger = window.isEmpty() ? txn(new BigDecimal("100000")) : window.get(0);
        when(history.byAccountBetween(eq(accountId), any(), any())).thenReturn(window);
        return new RuleContext(trigger, rule, history);
    }

    @Test
    void ruleCodeMatches() {
        assertThat(evaluator.ruleCode()).isEqualTo("ROUND_NUMBER");
    }

    @Test
    void firesWhenRoundAmountMeetsMinCount() {
        Transaction round = txn(new BigDecimal("100000"));
        RuleContext ctx = contextWith(rule(40, 24, 1), List.of(round));

        Optional<RuleHit> result = evaluator.evaluate(ctx);

        assertThat(result).isPresent();
        RuleHit hit = result.get();
        assertThat(hit.ruleCode()).isEqualTo("ROUND_NUMBER");
        assertThat(hit.riskWeight()).isEqualTo(40);
        assertThat(hit.evidenceTxnIds()).containsExactly(round.getId());
        assertThat(hit.explanation()).isNotBlank();
    }

    @Test
    void firesWhenMultipleRoundTxnsMeetMinCount() {
        Transaction r1 = txn(new BigDecimal("100000"));
        Transaction r2 = txn(new BigDecimal("300000"));
        RuleContext ctx = contextWith(rule(40, 24, 2), List.of(r1, r2));

        Optional<RuleHit> result = evaluator.evaluate(ctx);

        assertThat(result).isPresent();
        assertThat(result.get().evidenceTxnIds()).containsExactly(r1.getId(), r2.getId());
    }

    @Test
    void doesNotFireWhenAmountNotRound() {
        Transaction notRound = txn(new BigDecimal("123456"));
        RuleContext ctx = contextWith(rule(40, 24, 1), List.of(notRound));

        assertThat(evaluator.evaluate(ctx)).isEmpty();
    }

    @Test
    void doesNotFireWhenAmountIsZero() {
        Transaction zero = txn(BigDecimal.ZERO);
        RuleContext ctx = contextWith(rule(40, 24, 1), List.of(zero));

        assertThat(evaluator.evaluate(ctx)).isEmpty();
    }

    @Test
    void doesNotCountNullAmountTxn() {
        Transaction nullAmt = txn(null);
        RuleContext ctx = contextWith(rule(40, 24, 1), List.of(nullAmt));

        assertThat(evaluator.evaluate(ctx)).isEmpty();
    }

    @Test
    void doesNotFireWhenBelowMinCount() {
        Transaction round = txn(new BigDecimal("100000"));
        RuleContext ctx = contextWith(rule(40, 24, 2), List.of(round));

        assertThat(evaluator.evaluate(ctx)).isEmpty();
    }

    @Test
    void doesNotFireWhenWindowEmpty() {
        RuleContext ctx = contextWith(rule(40, 24, 1), List.of());

        assertThat(evaluator.evaluate(ctx)).isEmpty();
    }
}
