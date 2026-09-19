package com.customer.support.ai.appserver.repository;

import com.customer.support.ai.appserver.entity.Alert;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
    Optional<Alert> findByAccountIdAndRuleCodeAndStatus(UUID accountId, String ruleCode, String status);

    List<Alert> findByCaseId(UUID caseId);

    List<Alert> findByOrderByIdDesc(Limit limit);

    List<Alert> findByIdLessThanOrderByIdDesc(UUID id, Limit limit);

    List<Alert> findByStatusOrderByIdDesc(String status, Limit limit);

    List<Alert> findByStatusAndIdLessThanOrderByIdDesc(String status, UUID id, Limit limit);

    List<Alert> findByStatusInOrderByIdDesc(Collection<String> statuses, Limit limit);

    List<Alert> findByStatusInAndIdLessThanOrderByIdDesc(Collection<String> statuses, UUID id, Limit limit);

    long countByStatus(String status);

    long countByStatusIn(Collection<String> statuses);
}
