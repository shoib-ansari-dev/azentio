package com.customer.support.ai.appserver.controller;

import com.customer.support.ai.appserver.dto.AccountDetailResponse;
import com.customer.support.ai.appserver.dto.AccountUpdateRequest;
import com.customer.support.ai.appserver.dto.BulkUploadResponse;
import com.customer.support.ai.appserver.dto.CursorPage;
import com.customer.support.ai.appserver.dto.TransactionListItem;
import com.customer.support.ai.appserver.service.AccountService;
import com.customer.support.ai.appserver.service.BulkIngestionService;
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
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;
    private final BulkIngestionService bulkIngestionService;

    public AccountController(AccountService accountService, BulkIngestionService bulkIngestionService) {
        this.accountService = accountService;
        this.bulkIngestionService = bulkIngestionService;
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BulkUploadResponse> bulk(
            @RequestParam("file") MultipartFile file, Authentication authentication) {
        UUID jobId = bulkIngestionService.submit("ACCOUNTS", file, authentication.getName());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new BulkUploadResponse(jobId, "QUEUED", "Account bulk ingestion accepted"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ANALYST')")
    public AccountDetailResponse getById(@PathVariable UUID id) {
        return accountService.getById(id);
    }

    @GetMapping("/{id}/transactions")
    @PreAuthorize("hasRole('ANALYST')")
    public CursorPage<TransactionListItem> getTransactions(
            @PathVariable UUID id,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        return accountService.getTransactions(id, cursor, size);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public AccountDetailResponse update(@PathVariable UUID id, @Valid @RequestBody AccountUpdateRequest request) {
        return accountService.update(id, request);
    }
}
