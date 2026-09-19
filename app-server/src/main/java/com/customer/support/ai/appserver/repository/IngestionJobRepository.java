package com.customer.support.ai.appserver.repository;

import com.customer.support.ai.appserver.entity.IngestionJob;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestionJobRepository extends JpaRepository<IngestionJob, UUID> {
}
