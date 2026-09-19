package com.customer.support.ai.appserver.repository;

import com.customer.support.ai.appserver.entity.AuditLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
