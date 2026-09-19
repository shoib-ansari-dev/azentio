package com.customer.support.ai.appserver.repository;

import com.customer.support.ai.appserver.entity.Customer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByCustomerRef(String customerRef);

    Optional<Customer> findByEmail(String email);

    List<Customer> findByOrderByIdDesc(Limit limit);

    List<Customer> findByIdLessThanOrderByIdDesc(UUID id, Limit limit);
}
