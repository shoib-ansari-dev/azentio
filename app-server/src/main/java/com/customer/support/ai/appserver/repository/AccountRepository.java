package com.customer.support.ai.appserver.repository;

import com.customer.support.ai.appserver.entity.Account;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByAccountRef(String accountRef);
}
