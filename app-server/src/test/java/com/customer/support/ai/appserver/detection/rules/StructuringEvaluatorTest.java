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
class StructuringEvaluatorTest {

    private StructuringEvaluator evaluator;

    @Mock
    private TransactionHistory history;

    private final UUID accountId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.parse("2026-01-15T12:00:00Z");

    @BeforeEach
    void setUp() {
        evaluator = new StructuringEvaluator();
    }

    private DetectionRule rule() {
        DetectionRule r = new DetectionRule();
        r.setRuleCode("STRUCTURING");
        r.setRiskWeight(30);
        r.setParameters(
                "{\"window_hours\": 24, \"min_count\": 3, \"lower_inr\": 900000, \"upper_inr\": 999900}");
        return r;
    }

    private Transaction trigger() {
        Transaction t = new Transaction();
        t.setId(UUID.randomUUID());
        t.setAccountId(accountId);
        t.setTransactionTimestamp(now);
        return t;
    }

    private Transaction historic(BigDecimal amountInr) {
        Transaction t = new Transaction();
        t.setId(UUID.randomUUID());
        t.setAccountId(accountId);
        t.setTransactionTimestamp(now);
        t.setAmountInr(amountInr);
        return t;
    }

    @Test
    void ruleCodeIsStructuring() {
        assertThat(evaluator.ruleCode()).isEqualTo("STRUCTURING");
    }

    @Test
    void firesWhenCountMeetsMinCount() {
        Transaction t1 = historic(new BigDecimal("950000"));
        Transaction t2 = historic(new BigDecimal("960000"));
        Transaction t3 = historic(new BigDecimal("970000"));
        when(history.byAccountBetween(eq(accountId), any(), any()))
                .thenReturn(List.of(t1, t2, t3));
        RuleContext context = new RuleContext(trigger(), rule(), history);

        Optional<RuleHit> result = evaluator.evaluate(context);

        assertThat(result).isPresent();
        RuleHit hit = result.get();
        assertThat(hit.ruleCode()).isEqualTo("STRUCTURING");
        assertThat(hit.riskWeight()).isEqualTo(30);
        assertThat(hit.evidenceTxnIds())
                .containsExactly(t1.getId(), t2.getId(), t3.getId());
        assertThat(hit.explanation()).isNotBlank();
    }

    @Test
    void doesNotFireWhenBelowMinCount() {
        Transaction t1 = historic(new BigDecimal("950000"));
        Transaction t2 = historic(new BigDecimal("960000"));
        when(history.byAccountBetween(eq(accountId), any(), any()))
                .thenReturn(List.of(t1, t2));
        RuleContext context = new RuleContext(trigger(), rule(), history);

        assertThat(evaluator.evaluate(context)).isEmpty();
    }

    @Test
    void exactlyMinCountInWindowFires() {
        Transaction t1 = historic(new BigDecimal("900000"));
        Transaction t2 = historic(new BigDecimal("999900"));
        Transaction t3 = historic(new BigDecimal("950000"));
        when(history.byAccountBetween(eq(accountId), any(), any()))
                .thenReturn(List.of(t1, t2, t3));
        RuleContext context = new RuleContext(trigger(), rule(), history);

        assertThat(evaluator.evaluate(context)).isPresent();
    }

    @Test
    void amountsOutsideBandAreNotCounted() {
        Transaction inBand1 = historic(new BigDecimal("950000"));
        Transaction inBand2 = historic(new BigDecimal("960000"));
        Transaction tooLow = historic(new BigDecimal("899999"));
        Transaction tooHigh = historic(new BigDecimal("1000000"));
        when(history.byAccountBetween(eq(accountId), any(), any()))
                .thenReturn(List.of(inBand1, inBand2, tooLow, tooHigh));
        RuleContext context = new RuleContext(trigger(), rule(), history);

        assertThat(evaluator.evaluate(context)).isEmpty();
    }

    @Test
    void nullAmountIsSkippedWithoutNpe() {
        Transaction nullAmt = historic(null);
        Transaction t1 = historic(new BigDecimal("950000"));
        Transaction t2 = historic(new BigDecimal("960000"));
        Transaction t3 = historic(new BigDecimal("970000"));
        when(history.byAccountBetween(eq(accountId), any(), any()))
                .thenReturn(List.of(nullAmt, t1, t2, t3));
        RuleContext context = new RuleContext(trigger(), rule(), history);

        Optional<RuleHit> result = evaluator.evaluate(context);

        assertThat(result).isPresent();
        assertThat(result.get().evidenceTxnIds())
                .doesNotContain(nullAmt.getId())
                .containsExactly(t1.getId(), t2.getId(), t3.getId());
    }
}
