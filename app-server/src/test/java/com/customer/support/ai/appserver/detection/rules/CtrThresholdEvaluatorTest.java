package com.customer.support.ai.appserver.detection.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.customer.support.ai.appserver.detection.RuleContext;
import com.customer.support.ai.appserver.detection.RuleHit;
import com.customer.support.ai.appserver.entity.DetectionRule;
import com.customer.support.ai.appserver.entity.Transaction;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CtrThresholdEvaluatorTest {

    private CtrThresholdEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new CtrThresholdEvaluator();
    }

    private DetectionRule rule(int riskWeight, String threshold) {
        DetectionRule r = new DetectionRule();
        r.setRuleCode("CTR_THRESHOLD");
        r.setRiskWeight(riskWeight);
        r.setParameters("{\"threshold_inr\": " + threshold + "}");
        return r;
    }

    private Transaction txn(UUID id, BigDecimal amountInr) {
        Transaction t = new Transaction();
        t.setId(id);
        t.setAmountInr(amountInr);
        return t;
    }

    @Test
    void ruleCodeIsCtrThreshold() {
        assertThat(evaluator.ruleCode()).isEqualTo("CTR_THRESHOLD");
    }

    @Test
    void firesWhenAmountAboveThreshold() {
        UUID txnId = UUID.randomUUID();
        DetectionRule rule = rule(40, "1000000");
        Transaction txn = txn(txnId, new BigDecimal("1500000"));
        RuleContext context = new RuleContext(txn, rule, null);

        Optional<RuleHit> result = evaluator.evaluate(context);

        assertThat(result).isPresent();
        RuleHit hit = result.get();
        assertThat(hit.ruleCode()).isEqualTo("CTR_THRESHOLD");
        assertThat(hit.riskWeight()).isEqualTo(40);
        assertThat(hit.evidenceTxnIds()).containsExactly(txnId);
        assertThat(hit.explanation()).isNotBlank();
    }

    @Test
    void firesWhenAmountExactlyEqualToThreshold() {
        UUID txnId = UUID.randomUUID();
        DetectionRule rule = rule(40, "1000000");
        Transaction txn = txn(txnId, new BigDecimal("1000000"));
        RuleContext context = new RuleContext(txn, rule, null);

        Optional<RuleHit> result = evaluator.evaluate(context);

        assertThat(result).isPresent();
        assertThat(result.get().evidenceTxnIds()).containsExactly(txnId);
    }

    @Test
    void doesNotFireWhenBelowThreshold() {
        DetectionRule rule = rule(40, "1000000");
        Transaction txn = txn(UUID.randomUUID(), new BigDecimal("999999.99"));
        RuleContext context = new RuleContext(txn, rule, null);

        assertThat(evaluator.evaluate(context)).isEmpty();
    }

    @Test
    void doesNotFireAndNoNpeWhenAmountIsNull() {
        DetectionRule rule = rule(40, "1000000");
        Transaction txn = txn(UUID.randomUUID(), null);
        RuleContext context = new RuleContext(txn, rule, null);

        assertThat(evaluator.evaluate(context)).isEmpty();
    }
}
