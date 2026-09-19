package com.customer.support.ai.appserver.controller;

import com.customer.support.ai.appserver.dto.BulkUploadResponse;
import com.customer.support.ai.appserver.dto.TransactionCreateRequest;
import com.customer.support.ai.appserver.dto.TransactionCreateResponse;
import com.customer.support.ai.appserver.dto.TransactionDetailResponse;
import com.customer.support.ai.appserver.service.BulkIngestionService;
import com.customer.support.ai.appserver.service.IngestionService;
import com.customer.support.ai.appserver.service.TransactionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final IngestionService ingestionService;
    private final BulkIngestionService bulkIngestionService;

    public TransactionController(
            TransactionService transactionService,
            IngestionService ingestionService,
            BulkIngestionService bulkIngestionService) {
        this.transactionService = transactionService;
        this.ingestionService = ingestionService;
        this.bulkIngestionService = bulkIngestionService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ANALYST')")
    public TransactionDetailResponse getById(@PathVariable UUID id) {
        return transactionService.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransactionCreateResponse> create(
            @Valid @RequestBody TransactionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ingestionService.createSingle(request));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BulkUploadResponse> bulk(
            @RequestParam("file") MultipartFile file, Authentication authentication) {
        UUID jobId = bulkIngestionService.submit("TRANSACTIONS", file, authentication.getName());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new BulkUploadResponse(jobId, "QUEUED", "Transaction bulk ingestion accepted"));
    }
}
