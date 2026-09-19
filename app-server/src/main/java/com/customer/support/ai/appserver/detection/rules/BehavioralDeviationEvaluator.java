package com.customer.support.ai.appserver.detection.rules;

import com.customer.support.ai.appserver.detection.RuleContext;
import com.customer.support.ai.appserver.detection.RuleEvaluator;
import com.customer.support.ai.appserver.detection.RuleHit;
import com.customer.support.ai.appserver.entity.Transaction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class BehavioralDeviationEvaluator implements RuleEvaluator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String ruleCode() {
        return "BEHAVIORAL_DEVIATION";
    }

    @Override
    public Optional<RuleHit> evaluate(RuleContext context) {
        Transaction txn = context.transaction();
        if (txn.getAmountInr() == null) {
            return Optional.empty();
        }
        JsonNode params = readParams(context);
        int lookbackDays = params.get("lookback_days").asInt();
        BigDecimal multiplier = params.get("multiplier").decimalValue();

        OffsetDateTime to = txn.getTransactionTimestamp();
        OffsetDateTime from = to.minusDays(lookbackDays);
        List<Transaction> window = context.history().byAccountBetween(txn.getAccountId(), from, to);

        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (Transaction t : window) {
            if (t.getId().equals(txn.getId()) || t.getAmountInr() == null) {
                continue;
            }
            sum = sum.add(t.getAmountInr());
            count++;
        }
        if (count == 0) {
            return Optional.empty();
        }
        BigDecimal average = sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        BigDecimal threshold = average.multiply(multiplier);
        if (txn.getAmountInr().compareTo(threshold) <= 0) {
            return Optional.empty();
        }
        String explanation = String.format(
                "Transaction of ₹%s exceeds %sx the %d-day average of ₹%s",
                txn.getAmountInr().toPlainString(), multiplier.toPlainString(),
                lookbackDays, average.toPlainString());
        return Optional.of(new RuleHit(
                ruleCode(), context.rule().getRiskWeight(), explanation, List.of(txn.getId())));
    }

    private JsonNode readParams(RuleContext context) {
        try {
            return objectMapper.readTree(context.rule().getParameters());
        } catch (Exception e) {
            throw new IllegalStateException("Invalid BEHAVIORAL_DEVIATION parameters", e);
        }
    }
}
