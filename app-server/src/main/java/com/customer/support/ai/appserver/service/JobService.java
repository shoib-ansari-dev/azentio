package com.customer.support.ai.appserver.service;

import com.customer.support.ai.appserver.dto.CursorPage;
import com.customer.support.ai.appserver.dto.CursorSupport;
import com.customer.support.ai.appserver.dto.JobResponse;
import com.customer.support.ai.appserver.entity.IngestionJob;
import com.customer.support.ai.appserver.exception.EntityNotFoundException;
import com.customer.support.ai.appserver.repository.IngestionJobRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

@Service
public class JobService {

    private final IngestionJobRepository jobRepository;

    public JobService(IngestionJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public JobResponse getById(UUID id) {
        IngestionJob job = jobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Job not found"));
        return toResponse(job);
    }

    public CursorPage<JobResponse> list(String cursor, int size) {
        UUID cursorId = CursorSupport.decode(cursor);
        Limit limit = Limit.of(size + 1);
        List<IngestionJob> rows = cursorId == null
                ? jobRepository.findByOrderByIdDesc(limit)
                : jobRepository.findByIdLessThanOrderByIdDesc(cursorId, limit);
        long total = jobRepository.count();
        return CursorSupport.build(rows, size, total, IngestionJob::getId, JobService::toResponse);
    }

    private static JobResponse toResponse(IngestionJob job) {
        return new JobResponse(job.getId(), job.getJobType(), job.getStatus(),
                job.getTotalRecords(), job.getProcessed(), job.getFailed(),
                job.getErrorSummary(), job.getSubmittedBy(), job.getStartedAt(), job.getCompletedAt());
    }
}
