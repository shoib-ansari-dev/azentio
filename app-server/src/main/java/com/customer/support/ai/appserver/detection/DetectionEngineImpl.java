package com.customer.support.ai.appserver.detection;

import com.customer.support.ai.appserver.entity.DetectionRule;
import com.customer.support.ai.appserver.entity.Transaction;
import com.customer.support.ai.appserver.repository.DetectionRuleRepository;
import com.customer.support.ai.appserver.repository.TransactionRepository;
import com.customer.support.ai.appserver.service.AlertWriteService;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DetectionEngineImpl implements DetectionEngine {

    private final List<RuleEvaluator> evaluators;
    private final DetectionRuleRepository ruleRepository;
    private final TransactionRepository transactionRepository;
    private final AlertWriteService alertWriteService;
    private final Executor detectionExecutor;

    public DetectionEngineImpl(List<RuleEvaluator> evaluators, DetectionRuleRepository ruleRepository,
            TransactionRepository transactionRepository, AlertWriteService alertWriteService,
            @Qualifier("detectionExecutor") Executor detectionExecutor) {
        this.evaluators = evaluators;
        this.ruleRepository = ruleRepository;
        this.transactionRepository = transactionRepository;
        this.alertWriteService = alertWriteService;
        this.detectionExecutor = detectionExecutor;
    }

    @Override
    public void evaluate(Transaction transaction) {
        RuleContext.TransactionHistory history = transactionRepository::findByAccountIdAndTransactionTimestampBetween;
        List<CompletableFuture<Optional<RuleHit>>> futures = evaluators.stream()
                .map(evaluator -> runEvaluator(evaluator, transaction, history))
                .toList();
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        futures.stream()
                .map(CompletableFuture::join)
                .flatMap(Optional::stream)
                .forEach(hit -> alertWriteService.createOrMerge(transaction.getAccountId(), hit));
    }

    // fan out one evaluator on the detection pool; skip disabled/missing rules
    private CompletableFuture<Optional<RuleHit>> runEvaluator(
            RuleEvaluator evaluator, Transaction transaction, RuleContext.TransactionHistory history) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<DetectionRule> rule = ruleRepository.findByRuleCode(evaluator.ruleCode());
            if (rule.isEmpty() || !rule.get().isEnabled()) {
                return Optional.<RuleHit>empty();
            }
            return evaluator.evaluate(new RuleContext(transaction, rule.get(), history));
        }, detectionExecutor);
    }
}
