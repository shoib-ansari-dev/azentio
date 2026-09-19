package com.customer.support.ai.appserver.service;

import com.customer.support.ai.appserver.dto.AlertDismissRequest;
import com.customer.support.ai.appserver.dto.AlertEscalateRequest;
import com.customer.support.ai.appserver.dto.AlertResponse;
import com.customer.support.ai.appserver.dto.CursorPage;
import com.customer.support.ai.appserver.dto.CursorSupport;
import com.customer.support.ai.appserver.entity.Alert;
import com.customer.support.ai.appserver.entity.AmlCase;
import com.customer.support.ai.appserver.exception.EntityNotFoundException;
import com.customer.support.ai.appserver.repository.AccountRepository;
import com.customer.support.ai.appserver.repository.AlertRepository;
import com.customer.support.ai.appserver.repository.AmlCaseRepository;
import com.customer.support.ai.appserver.repository.CustomerRepository;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final AmlCaseRepository caseRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;

    public AlertService(AlertRepository alertRepository, AmlCaseRepository caseRepository,
                        CustomerRepository customerRepository, AccountRepository accountRepository) {
        this.alertRepository = alertRepository;
        this.caseRepository = caseRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
    }

    public CursorPage<AlertResponse> list(String status, String cursor, int size) {
        UUID cursorId = CursorSupport.decode(cursor);
        Limit limit = Limit.of(size + 1);
        List<Alert> rows;
        long total;
        if (status != null && !status.isBlank()) {
            Set<String> statuses = Arrays.stream(status.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
            if (statuses.size() == 1) {
                String single = statuses.iterator().next();
                rows = cursorId == null
                        ? alertRepository.findByStatusOrderByIdDesc(single, limit)
                        : alertRepository.findByStatusAndIdLessThanOrderByIdDesc(single, cursorId, limit);
                total = alertRepository.countByStatus(single);
            } else {
                rows = cursorId == null
                        ? alertRepository.findByStatusInOrderByIdDesc(statuses, limit)
                        : alertRepository.findByStatusInAndIdLessThanOrderByIdDesc(statuses, cursorId, limit);
                total = alertRepository.countByStatusIn(statuses);
            }
        } else {
            rows = cursorId == null
                    ? alertRepository.findByOrderByIdDesc(limit)
                    : alertRepository.findByIdLessThanOrderByIdDesc(cursorId, limit);
            total = alertRepository.count();
        }
        return CursorSupport.build(rows, size, total, Alert::getId, this::toResponse);
    }

    public AlertResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public AlertResponse acknowledge(UUID id) {
        Alert alert = findOrThrow(id);
        alert.setStatus("ACKNOWLEDGED");
        alert.setUpdatedAt(OffsetDateTime.now());
        return toResponse(alertRepository.save(alert));
    }

    @Transactional
    public AlertResponse dismiss(UUID id, AlertDismissRequest request) {
        Alert alert = findOrThrow(id);
        alert.setStatus("DISMISSED");
        alert.setDispositionReason(request.reason());
        alert.setUpdatedAt(OffsetDateTime.now());
        return toResponse(alertRepository.save(alert));
    }

    @Transactional
    public AlertResponse escalate(UUID id, AlertEscalateRequest request, String actor) {
        Alert alert = findOrThrow(id);
        AmlCase amlCase = request.caseId() == null
                ? createCase(alert, actor)
                : caseRepository.findById(request.caseId())
                        .orElseThrow(() -> new EntityNotFoundException("Case not found"));
        alert.setStatus("ESCALATED");
        alert.setCaseId(amlCase.getId());
        alert.setUpdatedAt(OffsetDateTime.now());
        return toResponse(alertRepository.save(alert));
    }

    private AmlCase createCase(Alert alert, String actor) {
        UUID caseId = UUID.randomUUID();
        AmlCase amlCase = new AmlCase();
        amlCase.setId(caseId);
        amlCase.setCaseRef("CASE_" + caseId.toString().substring(0, 8));
        amlCase.setTitle("Escalated: " + alert.getRuleCode() + " / " + alert.getAlertRef());
        amlCase.setStatus("OPEN");
        amlCase.setPriority(priorityFor(alert.getRiskScore()));
        amlCase.setCreatedBy(actor);
        OffsetDateTime now = OffsetDateTime.now();
        amlCase.setCreatedAt(now);
        amlCase.setUpdatedAt(now);
        return caseRepository.save(amlCase);
    }

    private static String priorityFor(int riskScore) {
        if (riskScore >= 90) {
            return "CRITICAL";
        }
        if (riskScore >= 70) {
            return "HIGH";
        }
        if (riskScore >= 40) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private Alert findOrThrow(UUID id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found"));
    }

    AlertResponse toResponse(Alert a) {
        String customerMasked = customerRepository.findById(a.getCustomerId())
                .map(c -> "****" + c.getLastName())
                .orElse(null);
        String accountRef = accountRepository.findById(a.getAccountId())
                .map(acc -> acc.getAccountRef())
                .orElse(null);
        return new AlertResponse(a.getId(), a.getAlertRef(), a.getAccountId(), a.getCustomerId(),
                a.getRuleCode(), a.getStatus(), a.getRiskScore(), a.getExplanation(),
                a.getEvidenceTxnIds(), a.getDispositionReason(), a.getAssignedTo(),
                a.getCaseId(), a.getCreatedAt(), a.getUpdatedAt(), customerMasked, accountRef);
    }
}
