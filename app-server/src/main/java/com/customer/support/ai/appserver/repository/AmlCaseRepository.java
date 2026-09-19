package com.customer.support.ai.appserver.repository;

import com.customer.support.ai.appserver.entity.AmlCase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmlCaseRepository extends JpaRepository<AmlCase, UUID> {
    Optional<AmlCase> findByCaseRef(String caseRef);

    List<AmlCase> findByOrderByIdDesc(Limit limit);

    List<AmlCase> findByIdLessThanOrderByIdDesc(UUID id, Limit limit);
}
