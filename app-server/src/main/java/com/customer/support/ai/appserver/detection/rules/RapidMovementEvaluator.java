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
public class RapidMovementEvaluator implements RuleEvaluator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String ruleCode() {
        return "RAPID_MOVEMENT";
    }

    @Override
    public Optional<RuleHit> evaluate(RuleContext context) {
        Transaction txn = context.transaction();
        JsonNode params = readParams(context);
        int windowHours = params.get("window_hours").asInt();
        BigDecimal outflowPct = params.get("outflow_pct").decimalValue();

        OffsetDateTime to = txn.getTransactionTimestamp();
        OffsetDateTime from = to.minusHours(windowHours);
        List<Transaction> window = context.history().byAccountBetween(txn.getAccountId(), from, to);

        BigDecimal inflow = BigDecimal.ZERO;
        BigDecimal outflow = BigDecimal.ZERO;
        List<UUID> evidence = new ArrayList<>();
        for (Transaction t : window) {
            BigDecimal amt = t.getAmountInr();
            if (amt == null) {
                continue;
            }
            if ("DEPOSIT".equals(t.getTransactionType())) {
                inflow = inflow.add(amt);
                evidence.add(t.getId());
            } else if (isOutflow(t.getTransactionType())) {
                outflow = outflow.add(amt);
                evidence.add(t.getId());
            }
        }
        if (inflow.signum() <= 0) {
            return Optional.empty();
        }
        BigDecimal actualPct = outflow.multiply(BigDecimal.valueOf(100))
                .divide(inflow, 2, java.math.RoundingMode.HALF_UP);
        if (actualPct.compareTo(outflowPct) < 0) {
            return Optional.empty();
        }
        if (!evidence.contains(txn.getId())) {
            evidence.add(txn.getId());
        }
        String explanation = String.format(
                "Outflow ₹%s is %s%% of inflow ₹%s within %d hours, indicating rapid movement",
                outflow.toPlainString(), actualPct.toPlainString(), inflow.toPlainString(), windowHours);
        return Optional.of(new RuleHit(
                ruleCode(), context.rule().getRiskWeight(), explanation, evidence));
    }

    private boolean isOutflow(String type) {
        return "WITHDRAWAL".equals(type) || "TRANSFER".equals(type) || "PAYMENT".equals(type);
    }

    private JsonNode readParams(RuleContext context) {
        try {
            return objectMapper.readTree(context.rule().getParameters());
        } catch (Exception e) {
            throw new IllegalStateException("Invalid RAPID_MOVEMENT parameters", e);
        }
    }
}
