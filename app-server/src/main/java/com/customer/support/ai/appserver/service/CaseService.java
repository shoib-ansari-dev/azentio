package com.customer.support.ai.appserver.service;

import com.customer.support.ai.appserver.dto.AlertResponse;
import com.customer.support.ai.appserver.dto.CaseCloseRequest;
import com.customer.support.ai.appserver.dto.CaseDetailResponse;
import com.customer.support.ai.appserver.dto.CaseResponse;
import com.customer.support.ai.appserver.dto.CaseUpdateRequest;
import com.customer.support.ai.appserver.dto.CursorPage;
import com.customer.support.ai.appserver.dto.CursorSupport;
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
public class CaseService {

    private final AmlCaseRepository caseRepository;
    private final AlertRepository alertRepository;
    private final AlertService alertService;

    public CaseService(AmlCaseRepository caseRepository, AlertRepository alertRepository, AlertService alertService) {
        this.caseRepository = caseRepository;
        this.alertRepository = alertRepository;
        this.alertService = alertService;
    }

    public CursorPage<CaseResponse> list(String cursor, int size) {
        UUID cursorId = CursorSupport.decode(cursor);
        Limit limit = Limit.of(size + 1);
        List<AmlCase> rows = cursorId == null
                ? caseRepository.findByOrderByIdDesc(limit)
                : caseRepository.findByIdLessThanOrderByIdDesc(cursorId, limit);
        return CursorSupport.build(rows, size, caseRepository.count(), AmlCase::getId, CaseService::toResponse);
    }

    public CaseDetailResponse getById(UUID id) {
        AmlCase amlCase = findOrThrow(id);
        List<AlertResponse> alerts = alertRepository.findByCaseId(id).stream()
                .map(alertService::toResponse).toList();
        return new CaseDetailResponse(toResponse(amlCase), alerts);
    }

    @Transactional
    public CaseResponse update(UUID id, CaseUpdateRequest request) {
        AmlCase amlCase = findOrThrow(id);
        if (request.assignedTo() != null) {
            amlCase.setAssignedTo(request.assignedTo());
        }
        if (request.notes() != null) {
            amlCase.setNotes(request.notes());
        }
        amlCase.setUpdatedAt(OffsetDateTime.now());
        return toResponse(caseRepository.save(amlCase));
    }

    @Transactional
    public CaseResponse close(UUID id, CaseCloseRequest request, String actor) {
        AmlCase amlCase = findOrThrow(id);
        amlCase.setStatus("CLOSED");
        amlCase.setDisposition(request.disposition());
        amlCase.setDispositionReason(request.dispositionReason());
        amlCase.setClosedBy(actor);
        OffsetDateTime now = OffsetDateTime.now();
        amlCase.setClosedAt(now);
        amlCase.setUpdatedAt(now);
        return toResponse(caseRepository.save(amlCase));
    }

    private AmlCase findOrThrow(UUID id) {
        return caseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Case not found"));
    }

    private static CaseResponse toResponse(AmlCase c) {
        return new CaseResponse(c.getId(), c.getCaseRef(), c.getTitle(), c.getStatus(), c.getPriority(),
                c.getAssignedTo(), c.getNotes(), c.getDisposition(), c.getDispositionReason(),
                c.getCreatedBy(), c.getClosedBy(), c.getCreatedAt(), c.getUpdatedAt(), c.getClosedAt());
    }
}
