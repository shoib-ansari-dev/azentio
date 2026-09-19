package com.customer.support.ai.appserver.repository;

import com.customer.support.ai.appserver.entity.ExchangeRate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {
    Optional<ExchangeRate> findByCurrency(String currency);
}
