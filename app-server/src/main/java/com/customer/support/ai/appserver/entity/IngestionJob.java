package com.customer.support.ai.appserver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ingestion_job")
@Getter
@Setter
public class IngestionJob {

    @Id
    private UUID id;

    @Column(name = "job_type", nullable = false)
    private String jobType;

    @Column(nullable = false)
    private String status;

    @Column(name = "total_records")
    private int totalRecords;

    private int processed;

    private int failed;

    @Column(name = "error_summary", columnDefinition = "text")
    private String errorSummary;

    @Column(name = "submitted_by")
    private String submittedBy;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
