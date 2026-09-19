package com.customer.support.ai.appserver.dto;

import com.customer.support.ai.appserver.entity.Customer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CustomerDetailResponse(
        UUID id,
        String customerRef,
        String firstName,
        String lastName,
        String gender,
        LocalDate dateOfBirth,
        String email,
        String phoneNumber,
        String city,
        String state,
        String country,
        String postalCode,
        String occupation,
        BigDecimal annualIncome,
        String maritalStatus,
        String educationLevel,
        String employmentStatus,
        LocalDate customerSince,
        String customerSegment,
        String kycStatus,
        String riskRating,
        boolean politicallyExposed,
        String preferredChannel,
        boolean emailVerified,
        boolean phoneVerified,
        Integer numComplaintsLastYear,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static CustomerDetailResponse from(Customer c) {
        return new CustomerDetailResponse(
                c.getId(),
                c.getCustomerRef(),
                c.getFirstName(),
                c.getLastName(),
                c.getGender(),
                c.getDateOfBirth(),
                c.getEmail(),
                c.getPhoneNumber(),
                c.getCity(),
                c.getState(),
                c.getCountry(),
                c.getPostalCode(),
                c.getOccupation(),
                c.getAnnualIncome(),
                c.getMaritalStatus(),
                c.getEducationLevel(),
                c.getEmploymentStatus(),
                c.getCustomerSince(),
                c.getCustomerSegment(),
                c.getKycStatus(),
                c.getRiskRating(),
                c.isPoliticallyExposed(),
                c.getPreferredChannel(),
                c.isEmailVerified(),
                c.isPhoneVerified(),
                c.getNumComplaintsLastYear(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
