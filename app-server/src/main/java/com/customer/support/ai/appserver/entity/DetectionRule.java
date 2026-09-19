package com.customer.support.ai.appserver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "detection_rule")
@Getter
@Setter
public class DetectionRule {

    @Id
    private UUID id;

    @Column(name = "rule_code", nullable = false, unique = true)
    private String ruleCode;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    private boolean enabled;

    @Column(name = "risk_weight", nullable = false)
    private int riskWeight;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String parameters;

    private int version;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
