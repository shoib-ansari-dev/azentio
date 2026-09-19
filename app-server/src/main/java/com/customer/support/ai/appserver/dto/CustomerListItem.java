package com.customer.support.ai.appserver.dto;

import com.customer.support.ai.appserver.entity.Customer;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerListItem(
        UUID id,
        String customerRef,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String dateOfBirth,
        String annualIncome,
        String gender,
        String city,
        String state,
        String country,
        String occupation,
        String maritalStatus,
        String educationLevel,
        String employmentStatus,
        LocalDate customerSince,
        String customerSegment,
        String kycStatus,
        String riskRating,
        boolean politicallyExposed,
        String preferredChannel) {

    private static final String REDACTED = "***";

    public static CustomerListItem from(Customer c) {
        return new CustomerListItem(
                c.getId(),
                c.getCustomerRef(),
                REDACTED,
                REDACTED,
                REDACTED,
                REDACTED,
                REDACTED,
                REDACTED,
                c.getGender(),
                c.getCity(),
                c.getState(),
                c.getCountry(),
                c.getOccupation(),
                c.getMaritalStatus(),
                c.getEducationLevel(),
                c.getEmploymentStatus(),
                c.getCustomerSince(),
                c.getCustomerSegment(),
                c.getKycStatus(),
                c.getRiskRating(),
                c.isPoliticallyExposed(),
                c.getPreferredChannel());
    }
}
