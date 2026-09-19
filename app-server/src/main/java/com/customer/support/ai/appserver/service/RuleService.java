package com.customer.support.ai.appserver.service;

import com.customer.support.ai.appserver.dto.RuleResponse;
import com.customer.support.ai.appserver.dto.RuleUpdateRequest;
import com.customer.support.ai.appserver.entity.DetectionRule;
import com.customer.support.ai.appserver.exception.EntityNotFoundException;
import com.customer.support.ai.appserver.repository.DetectionRuleRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuleService {

    private final DetectionRuleRepository ruleRepository;

    public RuleService(DetectionRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public List<RuleResponse> listAll() {
        return ruleRepository.findAll().stream().map(RuleService::toResponse).toList();
    }

    @Transactional
    public RuleResponse update(UUID id, RuleUpdateRequest request, String actor) {
        DetectionRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rule not found"));
        rule.setEnabled(request.enabled());
        rule.setParameters(request.parameters());
        rule.setVersion(rule.getVersion() + 1);
        rule.setUpdatedBy(actor);
        rule.setUpdatedAt(OffsetDateTime.now());
        return toResponse(ruleRepository.save(rule));
    }

    private static RuleResponse toResponse(DetectionRule r) {
        return new RuleResponse(r.getId(), r.getRuleCode(), r.getName(), r.getDescription(),
                r.isEnabled(), r.getRiskWeight(), r.getParameters(), r.getVersion(),
                r.getUpdatedBy(), r.getUpdatedAt());
    }
}
