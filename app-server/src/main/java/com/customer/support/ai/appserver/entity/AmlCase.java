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
@Table(name = "aml_case")
@Getter
@Setter
public class AmlCase {

    @Id
    private UUID id;

    @Column(name = "case_ref", nullable = false, unique = true)
    private String caseRef;

    @Column(nullable = false)
    private String title;

    private String status;
    private String priority;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(columnDefinition = "text")
    private String notes;

    private String disposition;

    @Column(name = "disposition_reason", columnDefinition = "text")
    private String dispositionReason;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "closed_by")
    private String closedBy;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;
}
