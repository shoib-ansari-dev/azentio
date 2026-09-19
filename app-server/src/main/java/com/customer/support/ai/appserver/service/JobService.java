package com.customer.support.ai.appserver.service;

import com.customer.support.ai.appserver.dto.JobResponse;
import com.customer.support.ai.appserver.entity.IngestionJob;
import com.customer.support.ai.appserver.exception.EntityNotFoundException;
import com.customer.support.ai.appserver.repository.IngestionJobRepository;
import java.util.UUID;
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
        return new JobResponse(job.getId(), job.getJobType(), job.getStatus(),
                job.getTotalRecords(), job.getProcessed(), job.getFailed(),
                job.getErrorSummary(), job.getSubmittedBy(), job.getStartedAt(), job.getCompletedAt());
    }
}
