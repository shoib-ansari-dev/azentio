package com.customer.support.ai.appserver.service;

import com.customer.support.ai.appserver.detection.RuleHit;
import com.customer.support.ai.appserver.entity.Account;
import com.customer.support.ai.appserver.entity.Alert;
import com.customer.support.ai.appserver.exception.EntityNotFoundException;
import com.customer.support.ai.appserver.repository.AccountRepository;
import com.customer.support.ai.appserver.repository.AlertRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertWriteService {

    private static final String OPEN = "OPEN";

    private final AlertRepository alertRepository;
    private final AccountRepository accountRepository;
    private final AuditService auditService;

    public AlertWriteService(AlertRepository alertRepository, AccountRepository accountRepository,
            AuditService auditService) {
        this.alertRepository = alertRepository;
        this.accountRepository = accountRepository;
        this.auditService = auditService;
    }

    // one OPEN alert per (account, rule_code); merge on hit, insert otherwise
    @Transactional
    public void createOrMerge(UUID accountId, RuleHit hit) {
        try {
            alertRepository.findByAccountIdAndRuleCodeAndStatus(accountId, hit.ruleCode(), OPEN)
                    .ifPresentOrElse(existing -> merge(existing, hit), () -> insert(accountId, hit));
        } catch (DataIntegrityViolationException concurrentInsert) {
            // constraint is the source of truth: a concurrent insert won, retry as merge
            Alert winner = alertRepository.findByAccountIdAndRuleCodeAndStatus(accountId, hit.ruleCode(), OPEN)
                    .orElseThrow(() -> concurrentInsert);
            merge(winner, hit);
        }
    }

    private void insert(UUID accountId, RuleHit hit) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        Alert alert = new Alert();
        alert.setId(id);
        alert.setAlertRef("ALRT_" + id.toString().substring(0, 8));
        alert.setAccountId(accountId);
        alert.setCustomerId(account.getCustomerId());
        alert.setRuleCode(hit.ruleCode());
        alert.setStatus(OPEN);
        alert.setRiskScore(cap(hit.riskWeight()));
        alert.setExplanation(hit.explanation());
        alert.setEvidenceTxnIds(new ArrayList<>(hit.evidenceTxnIds()));
        alert.setCreatedAt(now);
        alert.setUpdatedAt(now);
        alertRepository.save(alert);
        auditService.log("ALERT", id, "ALERT_CREATED", "system", null, alert.getAlertRef());
    }

    private void merge(Alert existing, RuleHit hit) {
        LinkedHashSet<UUID> evidence = new LinkedHashSet<>();
        if (existing.getEvidenceTxnIds() != null) {
            evidence.addAll(existing.getEvidenceTxnIds());
        }
        evidence.addAll(hit.evidenceTxnIds());
        existing.setEvidenceTxnIds(new ArrayList<>(evidence));
        existing.setRiskScore(Math.max(existing.getRiskScore(), cap(hit.riskWeight())));
        existing.setUpdatedAt(OffsetDateTime.now());
        alertRepository.save(existing);
    }

    private static int cap(int riskWeight) {
        return Math.min(100, Math.max(0, riskWeight));
    }
}
