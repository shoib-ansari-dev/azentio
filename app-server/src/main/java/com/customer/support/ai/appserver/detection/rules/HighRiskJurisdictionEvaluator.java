package com.customer.support.ai.appserver.detection.rules;

import com.customer.support.ai.appserver.detection.RuleContext;
import com.customer.support.ai.appserver.detection.RuleEvaluator;
import com.customer.support.ai.appserver.detection.RuleHit;
import com.customer.support.ai.appserver.entity.Transaction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class HighRiskJurisdictionEvaluator implements RuleEvaluator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String ruleCode() {
        return "HIGH_RISK_JURISDICTION";
    }

    @Override
    public Optional<RuleHit> evaluate(RuleContext context) {
        Transaction txn = context.transaction();
        String jurisdiction = txn.getCounterpartyJurisdiction();
        if (jurisdiction == null) {
            return Optional.empty();
        }
        List<String> flagged = readJurisdictions(context);
        if (!flagged.contains(jurisdiction)) {
            return Optional.empty();
        }
        String explanation = String.format(
                "Counterparty jurisdiction %s is on the high-risk list %s",
                jurisdiction, flagged);
        return Optional.of(new RuleHit(
                ruleCode(), context.rule().getRiskWeight(), explanation, List.of(txn.getId())));
    }

    private List<String> readJurisdictions(RuleContext context) {
        try {
            JsonNode params = objectMapper.readTree(context.rule().getParameters());
            List<String> result = new ArrayList<>();
            params.get("jurisdictions").forEach(node -> result.add(node.asText()));
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Invalid HIGH_RISK_JURISDICTION parameters", e);
        }
    }
}
