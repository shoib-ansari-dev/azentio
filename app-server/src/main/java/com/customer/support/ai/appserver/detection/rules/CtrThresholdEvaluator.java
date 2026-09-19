package com.customer.support.ai.appserver.detection.rules;

import com.customer.support.ai.appserver.detection.RuleContext;
import com.customer.support.ai.appserver.detection.RuleEvaluator;
import com.customer.support.ai.appserver.detection.RuleHit;
import com.customer.support.ai.appserver.entity.Transaction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CtrThresholdEvaluator implements RuleEvaluator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String ruleCode() {
        return "CTR_THRESHOLD";
    }

    @Override
    public Optional<RuleHit> evaluate(RuleContext context) {
        Transaction txn = context.transaction();
        BigDecimal threshold = readThreshold(context);
        if (txn.getAmountInr() == null || txn.getAmountInr().compareTo(threshold) < 0) {
            return Optional.empty();
        }
        String explanation = String.format(
                "Single transaction of ₹%s meets or exceeds CTR threshold ₹%s",
                txn.getAmountInr().toPlainString(), threshold.toPlainString());
        return Optional.of(new RuleHit(
                ruleCode(), context.rule().getRiskWeight(), explanation, List.of(txn.getId())));
    }

    private BigDecimal readThreshold(RuleContext context) {
        try {
            JsonNode params = objectMapper.readTree(context.rule().getParameters());
            return params.get("threshold_inr").decimalValue();
        } catch (Exception e) {
            throw new IllegalStateException("Invalid CTR_THRESHOLD parameters", e);
        }
    }
}
