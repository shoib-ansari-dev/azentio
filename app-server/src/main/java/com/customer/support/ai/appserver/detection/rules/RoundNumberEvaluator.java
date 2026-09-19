package com.customer.support.ai.appserver.detection.rules;

import com.customer.support.ai.appserver.detection.RuleContext;
import com.customer.support.ai.appserver.detection.RuleEvaluator;
import com.customer.support.ai.appserver.detection.RuleHit;
import com.customer.support.ai.appserver.entity.Transaction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RoundNumberEvaluator implements RuleEvaluator {

    private static final BigDecimal ROUND_UNIT = BigDecimal.valueOf(100000);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String ruleCode() {
        return "ROUND_NUMBER";
    }

    @Override
    public Optional<RuleHit> evaluate(RuleContext context) {
        Transaction txn = context.transaction();
        JsonNode params = readParams(context);
        int windowHours = params.get("window_hours").asInt();
        int minCount = params.get("min_count").asInt();

        OffsetDateTime to = txn.getTransactionTimestamp();
        OffsetDateTime from = to.minusHours(windowHours);
        List<Transaction> window = context.history().byAccountBetween(txn.getAccountId(), from, to);

        List<UUID> matched = new ArrayList<>();
        for (Transaction t : window) {
            BigDecimal amt = t.getAmountInr();
            if (amt != null && amt.signum() != 0 && amt.remainder(ROUND_UNIT).signum() == 0) {
                matched.add(t.getId());
            }
        }
        if (matched.size() < minCount) {
            return Optional.empty();
        }
        String explanation = String.format(
                "%d round-number transactions within %d hours indicate suspicious patterning",
                matched.size(), windowHours);
        return Optional.of(new RuleHit(
                ruleCode(), context.rule().getRiskWeight(), explanation, matched));
    }

    private JsonNode readParams(RuleContext context) {
        try {
            return objectMapper.readTree(context.rule().getParameters());
        } catch (Exception e) {
            throw new IllegalStateException("Invalid ROUND_NUMBER parameters", e);
        }
    }
}
