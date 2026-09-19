package com.customer.support.ai.appserver.service;

import com.customer.support.ai.appserver.dto.CursorPage;
import com.customer.support.ai.appserver.dto.CursorSupport;
import com.customer.support.ai.appserver.dto.CustomerDetailResponse;
import com.customer.support.ai.appserver.dto.CustomerListItem;
import com.customer.support.ai.appserver.dto.CustomerUpdateRequest;
import com.customer.support.ai.appserver.entity.Customer;
import com.customer.support.ai.appserver.exception.EntityNotFoundException;
import com.customer.support.ai.appserver.repository.CustomerRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public CustomerDetailResponse getById(UUID id) {
        return CustomerDetailResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public CursorPage<CustomerListItem> list(String cursor, int size) {
        UUID cursorId = CursorSupport.decode(cursor);
        Limit limit = Limit.of(size + 1);
        List<Customer> rows = cursorId == null
                ? customerRepository.findByOrderByIdDesc(limit)
                : customerRepository.findByIdLessThanOrderByIdDesc(cursorId, limit);
        long total = customerRepository.count();
        return CursorSupport.build(rows, size, total, Customer::getId, CustomerListItem::from);
    }

    @Transactional
    public CustomerDetailResponse update(UUID id, CustomerUpdateRequest request) {
        Customer customer = findOrThrow(id);
        customer.setKycStatus(request.kycStatus());
        customer.setRiskRating(request.riskRating());
        customer.setUpdatedAt(OffsetDateTime.now());
        return CustomerDetailResponse.from(customerRepository.save(customer));
    }

    private Customer findOrThrow(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
    }
}
