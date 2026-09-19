package com.customer.support.ai.appserver.service;

import com.customer.support.ai.appserver.dto.AlertDismissRequest;
import com.customer.support.ai.appserver.dto.AlertEscalateRequest;
import com.customer.support.ai.appserver.dto.AlertResponse;
import com.customer.support.ai.appserver.dto.CursorPage;
import com.customer.support.ai.appserver.dto.CursorSupport;
import com.customer.support.ai.appserver.entity.Alert;
import com.customer.support.ai.appserver.entity.AmlCase;
import com.customer.support.ai.appserver.exception.EntityNotFoundException;
import com.customer.support.ai.appserver.repository.AlertRepository;
import com.customer.support.ai.appserver.repository.AmlCaseRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final AmlCaseRepository caseRepository;

    public AlertService(AlertRepository alertRepository, AmlCaseRepository caseRepository) {
        this.alertRepository = alertRepository;
        this.caseRepository = caseRepository;
    }

    public CursorPage<AlertResponse> list(String status, String cursor, int size) {
        UUID cursorId = CursorSupport.decode(cursor);
        Limit limit = Limit.of(size + 1);
        List<Alert> rows;
        long total;
        if (status != null && !status.isBlank()) {
            rows = cursorId == null
                    ? alertRepository.findByStatusOrderByIdDesc(status, limit)
                    : alertRepository.findByStatusAndIdLessThanOrderByIdDesc(status, cursorId, limit);
            total = alertRepository.countByStatus(status);
        } else {
            rows = cursorId == null
                    ? alertRepository.findByOrderByIdDesc(limit)
                    : alertRepository.findByIdLessThanOrderByIdDesc(cursorId, limit);
            total = alertRepository.count();
        }
        return CursorSupport.build(rows, size, total, Alert::getId, AlertService::toResponse);
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

    static AlertResponse toResponse(Alert a) {
        return new AlertResponse(a.getId(), a.getAlertRef(), a.getAccountId(), a.getCustomerId(),
                a.getRuleCode(), a.getStatus(), a.getRiskScore(), a.getExplanation(),
                a.getEvidenceTxnIds(), a.getDispositionReason(), a.getAssignedTo(),
                a.getCaseId(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
