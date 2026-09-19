package com.customer.support.ai.appserver.detection.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.customer.support.ai.appserver.detection.RuleContext;
import com.customer.support.ai.appserver.detection.RuleContext.TransactionHistory;
import com.customer.support.ai.appserver.detection.RuleHit;
import com.customer.support.ai.appserver.entity.DetectionRule;
import com.customer.support.ai.appserver.entity.Transaction;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RapidMovementEvaluatorTest {

    private RapidMovementEvaluator evaluator;

    @Mock
    private TransactionHistory history;

    private final UUID accountId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.parse("2026-01-15T12:00:00Z");

    @BeforeEach
    void setUp() {
        evaluator = new RapidMovementEvaluator();
    }

    private DetectionRule rule() {
        DetectionRule r = new DetectionRule();
        r.setRuleCode("RAPID_MOVEMENT");
        r.setRiskWeight(50);
        r.setParameters("{\"window_hours\": 48, \"outflow_pct\": 80}");
        return r;
    }

    private Transaction trigger() {
        Transaction t = new Transaction();
        t.setId(UUID.randomUUID());
        t.setAccountId(accountId);
        t.setTransactionTimestamp(now);
        return t;
    }

    private Transaction tx(String type, BigDecimal amountInr) {
        Transaction t = new Transaction();
        t.setId(UUID.randomUUID());
        t.setAccountId(accountId);
        t.setTransactionTimestamp(now);
        t.setTransactionType(type);
        t.setAmountInr(amountInr);
        return t;
    }

    @Test
    void ruleCodeIsRapidMovement() {
        assertThat(evaluator.ruleCode()).isEqualTo("RAPID_MOVEMENT");
    }

    @Test
    void firesWhenOutflowExceedsPct() {
        Transaction deposit = tx("DEPOSIT", new BigDecimal("1000000"));
        Transaction withdrawal = tx("WITHDRAWAL", new BigDecimal("900000"));
        when(history.byAccountBetween(eq(accountId), any(), any()))
                .thenReturn(List.of(deposit, withdrawal));
        Transaction trigger = trigger();
        RuleContext context = new RuleContext(trigger, rule(), history);

        Optional<RuleHit> result = evaluator.evaluate(context);

        assertThat(result).isPresent();
        RuleHit hit = result.get();
        assertThat(hit.ruleCode()).isEqualTo("RAPID_MOVEMENT");
        assertThat(hit.riskWeight()).isEqualTo(50);
        assertThat(hit.evidenceTxnIds())
                .contains(deposit.getId(), withdrawal.getId(), trigger.getId());
        assertThat(hit.explanation()).isNotBlank();
    }

    @Test
    void firesWhenOutflowExactlyEqualsPctBoundary() {
        Transaction deposit = tx("DEPOSIT", new BigDecimal("1000000"));
        Transaction transfer = tx("TRANSFER", new BigDecimal("800000"));
        when(history.byAccountBetween(eq(accountId), any(), any()))
                .thenReturn(List.of(deposit, transfer));
        RuleContext context = new RuleContext(trigger(), rule(), history);

        assertThat(evaluator.evaluate(context)).isPresent();
    }

    @Test
    void doesNotFireWhenOutflowBelowPctBoundary() {
        Transaction deposit = tx("DEPOSIT", new BigDecimal("1000000"));
        Transaction payment = tx("PAYMENT", new BigDecimal("790000"));
        when(history.byAccountBetween(eq(accountId), any(), any()))
                .thenReturn(List.of(deposit, payment));
        RuleContext context = new RuleContext(trigger(), rule(), history);

        assertThat(evaluator.evaluate(context)).isEmpty();
    }

    @Test
    void doesNotFireWhenNoInflow() {
        Transaction withdrawal = tx("WITHDRAWAL", new BigDecimal("900000"));
        when(history.byAccountBetween(eq(accountId), any(), any()))
                .thenReturn(List.of(withdrawal));
        RuleContext context = new RuleContext(trigger(), rule(), history);

        assertThat(evaluator.evaluate(context)).isEmpty();
    }

    @Test
    void doesNotFireWhenHistoryEmpty() {
        when(history.byAccountBetween(eq(accountId), any(), any()))
                .thenReturn(List.of());
        RuleContext context = new RuleContext(trigger(), rule(), history);

        assertThat(evaluator.evaluate(context)).isEmpty();
    }

    @Test
    void nullAmountIsSkippedWithoutNpe() {
        Transaction nullAmt = tx("WITHDRAWAL", null);
        Transaction deposit = tx("DEPOSIT", new BigDecimal("1000000"));
        Transaction withdrawal = tx("WITHDRAWAL", new BigDecimal("900000"));
        when(history.byAccountBetween(eq(accountId), any(), any()))
                .thenReturn(List.of(nullAmt, deposit, withdrawal));
        RuleContext context = new RuleContext(trigger(), rule(), history);

        Optional<RuleHit> result = evaluator.evaluate(context);

        assertThat(result).isPresent();
        assertThat(result.get().evidenceTxnIds()).doesNotContain(nullAmt.getId());
    }
}
