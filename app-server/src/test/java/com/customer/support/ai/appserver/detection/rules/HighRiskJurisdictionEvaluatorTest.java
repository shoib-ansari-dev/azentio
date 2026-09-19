package com.customer.support.ai.appserver.detection.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.customer.support.ai.appserver.detection.RuleContext;
import com.customer.support.ai.appserver.detection.RuleHit;
import com.customer.support.ai.appserver.entity.DetectionRule;
import com.customer.support.ai.appserver.entity.Transaction;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HighRiskJurisdictionEvaluatorTest {

    private final HighRiskJurisdictionEvaluator evaluator = new HighRiskJurisdictionEvaluator();
    private final RuleContext.TransactionHistory history = mock(RuleContext.TransactionHistory.class);

    private DetectionRule rule(int riskWeight, String jurisdictionsJson) {
        DetectionRule rule = new DetectionRule();
        rule.setRuleCode("HIGH_RISK_JURISDICTION");
        rule.setRiskWeight(riskWeight);
        rule.setParameters("{\"jurisdictions\": " + jurisdictionsJson + "}");
        return rule;
    }

    private Transaction txn(String jurisdiction) {
        Transaction txn = new Transaction();
        txn.setId(UUID.randomUUID());
        txn.setAccountId(UUID.randomUUID());
        txn.setAmountInr(new BigDecimal("1000"));
        txn.setCounterpartyJurisdiction(jurisdiction);
        txn.setTransactionTimestamp(OffsetDateTime.now());
        return txn;
    }

    @Test
    void ruleCodeMatches() {
        assertThat(evaluator.ruleCode()).isEqualTo("HIGH_RISK_JURISDICTION");
    }

    @Test
    void firesWhenJurisdictionOnList() {
        Transaction txn = txn("IR");
        DetectionRule rule = rule(70, "[\"IR\", \"KP\"]");
        RuleContext ctx = new RuleContext(txn, rule, history);

        Optional<RuleHit> result = evaluator.evaluate(ctx);

        assertThat(result).isPresent();
        RuleHit hit = result.get();
        assertThat(hit.ruleCode()).isEqualTo("HIGH_RISK_JURISDICTION");
        assertThat(hit.riskWeight()).isEqualTo(70);
        assertThat(hit.riskWeight()).isEqualTo(rule.getRiskWeight());
        assertThat(hit.evidenceTxnIds()).containsExactly(txn.getId());
        assertThat(hit.explanation()).isNotBlank();
    }

    @Test
    void firesRegardlessOfAmount() {
        Transaction txn = txn("KP");
        txn.setAmountInr(BigDecimal.ZERO);
        RuleContext ctx = new RuleContext(txn, rule(50, "[\"KP\"]"), history);

        assertThat(evaluator.evaluate(ctx)).isPresent();
    }

    @Test
    void doesNotFireWhenJurisdictionNotOnList() {
        Transaction txn = txn("US");
        RuleContext ctx = new RuleContext(txn, rule(70, "[\"IR\", \"KP\"]"), history);

        assertThat(evaluator.evaluate(ctx)).isEmpty();
    }

    @Test
    void doesNotFireWhenJurisdictionNull() {
        Transaction txn = txn(null);
        RuleContext ctx = new RuleContext(txn, rule(70, "[\"IR\", \"KP\"]"), history);

        assertThat(evaluator.evaluate(ctx)).isEmpty();
    }

    @Test
    void doesNotFireWhenJurisdictionBlank() {
        Transaction txn = txn("");
        RuleContext ctx = new RuleContext(txn, rule(70, "[\"IR\", \"KP\"]"), history);

        assertThat(evaluator.evaluate(ctx)).isEmpty();
    }
}
