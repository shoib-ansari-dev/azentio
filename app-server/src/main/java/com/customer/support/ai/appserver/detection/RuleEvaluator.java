package com.customer.support.ai.appserver.detection;

import java.util.Optional;

public interface RuleEvaluator {

    String ruleCode();

    Optional<RuleHit> evaluate(RuleContext context);
}
