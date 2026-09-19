package com.customer.support.ai.appserver.repository;

import com.customer.support.ai.appserver.entity.Transaction;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByTransactionRef(String transactionRef);

    List<Transaction> findByAccountIdAndTransactionTimestampBetween(
            UUID accountId, OffsetDateTime from, OffsetDateTime to);

    Page<Transaction> findByAccountId(UUID accountId, Pageable pageable);

    List<Transaction> findByAccountIdOrderByIdDesc(UUID accountId, Limit limit);

    List<Transaction> findByAccountIdAndIdLessThanOrderByIdDesc(UUID accountId, UUID id, Limit limit);

    long countByAccountId(UUID accountId);
}
