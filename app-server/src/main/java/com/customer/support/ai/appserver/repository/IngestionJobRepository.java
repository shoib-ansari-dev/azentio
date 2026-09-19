package com.customer.support.ai.appserver.repository;

import com.customer.support.ai.appserver.entity.IngestionJob;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestionJobRepository extends JpaRepository<IngestionJob, UUID> {
    List<IngestionJob> findByOrderByIdDesc(Limit limit);

    List<IngestionJob> findByIdLessThanOrderByIdDesc(UUID id, Limit limit);
}
