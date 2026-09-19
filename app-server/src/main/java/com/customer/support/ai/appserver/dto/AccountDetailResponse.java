package com.customer.support.ai.appserver.dto;

import com.customer.support.ai.appserver.entity.Account;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountDetailResponse(
        UUID id,
        String accountRef,
        UUID customerId,
        String accountType,
        String accountStatus,
        String currency,
        LocalDate openDate,
        LocalDate closeDate,
        String branchCode,
        String branchCity,
        BigDecimal currentBalance,
        BigDecimal avgMonthlyBalance6m,
        BigDecimal creditLimit,
        BigDecimal creditUtilizationPct,
        boolean overdraftEnabled,
        String cardType,
        boolean jointAccount,
        Integer numLinkedDevices,
        boolean mobileBankingEnrolled,
        LocalDate lastLoginDate,
        Integer avgMonthlyTxnCount,
        String accountTier,
        String riskRating,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static AccountDetailResponse from(Account a) {
        return new AccountDetailResponse(
                a.getId(),
                a.getAccountRef(),
                a.getCustomerId(),
                a.getAccountType(),
                a.getAccountStatus(),
                a.getCurrency(),
                a.getOpenDate(),
                a.getCloseDate(),
                a.getBranchCode(),
                a.getBranchCity(),
                a.getCurrentBalance(),
                a.getAvgMonthlyBalance6m(),
                a.getCreditLimit(),
                a.getCreditUtilizationPct(),
                a.isOverdraftEnabled(),
                a.getCardType(),
                a.isJointAccount(),
                a.getNumLinkedDevices(),
                a.isMobileBankingEnrolled(),
                a.getLastLoginDate(),
                a.getAvgMonthlyTxnCount(),
                a.getAccountTier(),
                a.getRiskRating(),
                a.getCreatedAt(),
                a.getUpdatedAt());
    }
}
