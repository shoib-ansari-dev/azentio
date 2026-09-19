package com.customer.support.ai.appserver.service;

import com.customer.support.ai.appserver.detection.CurrencyConverter;
import com.customer.support.ai.appserver.detection.DetectionEngine;
import com.customer.support.ai.appserver.dto.AlertResponse;
import com.customer.support.ai.appserver.dto.TransactionCreateRequest;
import com.customer.support.ai.appserver.dto.TransactionCreateResponse;
import com.customer.support.ai.appserver.dto.TransactionDetailResponse;
import com.customer.support.ai.appserver.entity.Alert;
import com.customer.support.ai.appserver.entity.Transaction;
import com.customer.support.ai.appserver.repository.AlertRepository;
import com.customer.support.ai.appserver.repository.TransactionRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngestionService {

    private static final List<String> RULE_CODES = List.of(
            "CTR_THRESHOLD", "STRUCTURING", "RAPID_MOVEMENT",
            "HIGH_RISK_JURISDICTION", "BEHAVIORAL_DEVIATION", "ROUND_NUMBER");

    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;
    private final CurrencyConverter currencyConverter;
    private final DetectionEngine detectionEngine;

    public IngestionService(
            TransactionRepository transactionRepository,
            AlertRepository alertRepository,
            CurrencyConverter currencyConverter,
            DetectionEngine detectionEngine) {
        this.transactionRepository = transactionRepository;
        this.alertRepository = alertRepository;
        this.currencyConverter = currencyConverter;
        this.detectionEngine = detectionEngine;
    }

    @Transactional
    public TransactionCreateResponse createSingle(TransactionCreateRequest request) {
        Transaction transaction = toEntity(request);
        Transaction saved = transactionRepository.save(transaction);
        detectionEngine.evaluate(saved);
        return new TransactionCreateResponse(
                TransactionDetailResponse.from(saved), openAlerts(saved.getAccountId()));
    }

    private Transaction toEntity(TransactionCreateRequest request) {
        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setTransactionRef(request.transactionRef());
        transaction.setAccountId(request.accountId());
        transaction.setAmount(request.amount());
        transaction.setCurrency(request.currency());
        CurrencyConverter.ConversionResult conversion =
                currencyConverter.toInr(request.currency(), request.amount());
        transaction.setAmountInr(conversion.amountInr());
        transaction.setExchangeRateUsed(conversion.rateUsed());
        transaction.setTransactionType(request.transactionType());
        transaction.setChannel(request.channel());
        transaction.setCounterpartyAccount(request.counterpartyAccount());
        transaction.setCounterpartyBank(request.counterpartyBank());
        transaction.setCounterpartyJurisdiction(request.counterpartyJurisdiction());
        transaction.setDescription(request.description());
        transaction.setTransactionTimestamp(request.transactionTimestamp());
        transaction.setIngestedAt(OffsetDateTime.now());
        transaction.setStatus("PROCESSED");
        return transaction;
    }

    private List<AlertResponse> openAlerts(UUID accountId) {
        List<AlertResponse> alerts = new ArrayList<>();
        for (String ruleCode : RULE_CODES) {
            alertRepository.findByAccountIdAndRuleCodeAndStatus(accountId, ruleCode, "OPEN")
                    .map(IngestionService::toResponse)
                    .ifPresent(alerts::add);
        }
        return alerts;
    }

    private static AlertResponse toResponse(Alert a) {
        return new AlertResponse(a.getId(), a.getAlertRef(), a.getAccountId(), a.getCustomerId(),
                a.getRuleCode(), a.getStatus(), a.getRiskScore(), a.getExplanation(),
                a.getEvidenceTxnIds(), a.getDispositionReason(), a.getAssignedTo(),
                a.getCaseId(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
