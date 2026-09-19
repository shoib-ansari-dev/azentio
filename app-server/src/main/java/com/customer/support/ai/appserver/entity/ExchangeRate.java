package com.customer.support.ai.appserver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "exchange_rate")
@Getter
@Setter
public class ExchangeRate {

    @Id
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "char(3)", nullable = false, unique = true)
    private String currency;

    @Column(name = "rate_to_inr", nullable = false)
    private BigDecimal rateToInr;

    @Column(name = "effective_from")
    private OffsetDateTime effectiveFrom;

    @Column(name = "updated_by")
    private String updatedBy;
}
