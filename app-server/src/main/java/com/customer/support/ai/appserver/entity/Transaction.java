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
@Table(name = "transaction")
@Getter
@Setter
public class Transaction {

    @Id
    private UUID id;

    @Column(name = "transaction_ref", nullable = false, unique = true)
    private String transactionRef;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(nullable = false)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "char(3)", nullable = false)
    private String currency;

    @Column(name = "amount_inr")
    private BigDecimal amountInr;

    @Column(name = "exchange_rate_used")
    private BigDecimal exchangeRateUsed;

    @Column(name = "transaction_type")
    private String transactionType;

    private String channel;

    @Column(name = "counterparty_account")
    private String counterpartyAccount;

    @Column(name = "counterparty_bank")
    private String counterpartyBank;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "counterparty_jurisdiction", columnDefinition = "char(2)")
    private String counterpartyJurisdiction;

    private String description;

    @Column(name = "transaction_timestamp", nullable = false)
    private OffsetDateTime transactionTimestamp;

    @Column(name = "ingested_at")
    private OffsetDateTime ingestedAt;

    private String status;

    @Column(name = "job_id")
    private UUID jobId;
}
