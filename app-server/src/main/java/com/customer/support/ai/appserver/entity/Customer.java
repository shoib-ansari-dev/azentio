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
@Table(name = "customer")
@Getter
@Setter
public class Customer {

    @Id
    private UUID id;

    @Column(name = "customer_ref", nullable = false, unique = true)
    private String customerRef;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "char(1)")
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String city;
    private String state;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "char(2)")
    private String country;

    @Column(name = "postal_code")
    private String postalCode;

    private String occupation;

    @Column(name = "annual_income")
    private BigDecimal annualIncome;

    @Column(name = "marital_status")
    private String maritalStatus;

    @Column(name = "education_level")
    private String educationLevel;

    @Column(name = "employment_status")
    private String employmentStatus;

    @Column(name = "customer_since")
    private LocalDate customerSince;

    @Column(name = "customer_segment")
    private String customerSegment;

    @Column(name = "kyc_status")
    private String kycStatus;

    @Column(name = "risk_rating")
    private String riskRating;

    @Column(name = "is_politically_exposed")
    private boolean politicallyExposed;

    @Column(name = "preferred_channel")
    private String preferredChannel;

    @Column(name = "email_verified")
    private boolean emailVerified;

    @Column(name = "phone_verified")
    private boolean phoneVerified;

    @Column(name = "num_complaints_last_year")
    private Integer numComplaintsLastYear;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
