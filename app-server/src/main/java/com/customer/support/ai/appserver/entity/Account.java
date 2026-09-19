package com.customer.support.ai.appserver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "account")
@Getter
@Setter
public class Account {

    @Id
    private UUID id;

    @Column(name = "account_ref", nullable = false, unique = true)
    private String accountRef;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "account_type")
    private String accountType;

    @Column(name = "account_status")
    private String accountStatus;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "char(3)")
    private String currency;

    @Column(name = "open_date")
    private LocalDate openDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(name = "branch_code")
    private String branchCode;

    @Column(name = "branch_city")
    private String branchCity;

    @Column(name = "current_balance")
    private BigDecimal currentBalance;

    @Column(name = "avg_monthly_balance_6m")
    private BigDecimal avgMonthlyBalance6m;

    @Column(name = "credit_limit")
    private BigDecimal creditLimit;

    @Column(name = "credit_utilization_pct")
    private BigDecimal creditUtilizationPct;

    @Column(name = "overdraft_enabled")
    private boolean overdraftEnabled;

    @Column(name = "card_type")
    private String cardType;

    @Column(name = "is_joint_account")
    private boolean jointAccount;

    @Column(name = "num_linked_devices")
    private Integer numLinkedDevices;

    @Column(name = "mobile_banking_enrolled")
    private boolean mobileBankingEnrolled;

    @Column(name = "last_login_date")
    private LocalDate lastLoginDate;

    @Column(name = "avg_monthly_txn_count")
    private Integer avgMonthlyTxnCount;

    @Column(name = "account_tier")
    private String accountTier;

    @Column(name = "risk_rating")
    private String riskRating;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
