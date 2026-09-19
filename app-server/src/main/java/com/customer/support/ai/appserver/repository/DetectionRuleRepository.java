package com.customer.support.ai.appserver.repository;

import com.customer.support.ai.appserver.entity.DetectionRule;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetectionRuleRepository extends JpaRepository<DetectionRule, UUID> {
    Optional<DetectionRule> findByRuleCode(String ruleCode);
}
