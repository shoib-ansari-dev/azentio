package com.customer.support.ai.appserver.service;

import com.customer.support.ai.appserver.dto.TransactionDetailResponse;
import com.customer.support.ai.appserver.exception.EntityNotFoundException;
import com.customer.support.ai.appserver.repository.TransactionRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public TransactionDetailResponse getById(UUID id) {
        return transactionRepository.findById(id)
                .map(TransactionDetailResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + id));
    }
}
