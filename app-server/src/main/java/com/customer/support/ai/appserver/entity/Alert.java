package com.customer.support.ai.appserver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "alert")
@Getter
@Setter
public class Alert {

    @Id
    private UUID id;

    @Column(name = "alert_ref", nullable = false, unique = true)
    private String alertRef;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "rule_code", nullable = false)
    private String ruleCode;

    private String status;

    @Column(name = "risk_score")
    private int riskScore;

    @Column(columnDefinition = "text")
    private String explanation;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "evidence_txn_ids", columnDefinition = "uuid[]")
    private List<UUID> evidenceTxnIds;

    @Column(name = "disposition_reason", columnDefinition = "text")
    private String dispositionReason;

    @Column(name = "assigned_to")
    private String assignedTo;

    @Column(name = "case_id")
    private UUID caseId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
