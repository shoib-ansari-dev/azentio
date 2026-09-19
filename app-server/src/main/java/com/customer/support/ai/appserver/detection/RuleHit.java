package com.customer.support.ai.appserver.detection;

import java.util.List;
import java.util.UUID;

public record RuleHit(
        String ruleCode,
        int riskWeight,
        String explanation,
        List<UUID> evidenceTxnIds) {
}
