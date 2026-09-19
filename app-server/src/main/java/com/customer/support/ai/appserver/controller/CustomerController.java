package com.customer.support.ai.appserver.controller;

import com.customer.support.ai.appserver.dto.BulkUploadResponse;
import com.customer.support.ai.appserver.dto.CursorPage;
import com.customer.support.ai.appserver.dto.CustomerDetailResponse;
import com.customer.support.ai.appserver.dto.CustomerListItem;
import com.customer.support.ai.appserver.dto.CustomerUpdateRequest;
import com.customer.support.ai.appserver.service.BulkIngestionService;
import com.customer.support.ai.appserver.service.CustomerService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final BulkIngestionService bulkIngestionService;

    public CustomerController(CustomerService customerService, BulkIngestionService bulkIngestionService) {
        this.customerService = customerService;
        this.bulkIngestionService = bulkIngestionService;
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BulkUploadResponse> bulk(
            @RequestParam("file") MultipartFile file, Authentication authentication) {
        UUID jobId = bulkIngestionService.submit("CUSTOMERS", file, authentication.getName());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new BulkUploadResponse(jobId, "QUEUED", "Customer bulk ingestion accepted"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ANALYST')")
    public CustomerDetailResponse getById(@PathVariable UUID id) {
        return customerService.getById(id);
    }

    @GetMapping
    @PreAuthorize("hasRole('ANALYST')")
    public CursorPage<CustomerListItem> list(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        return customerService.list(cursor, size);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public CustomerDetailResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerUpdateRequest request) {
        return customerService.update(id, request);
    }
}
