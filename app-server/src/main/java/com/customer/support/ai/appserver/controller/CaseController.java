package com.customer.support.ai.appserver.controller;

import com.customer.support.ai.appserver.dto.CaseCloseRequest;
import com.customer.support.ai.appserver.dto.CaseDetailResponse;
import com.customer.support.ai.appserver.dto.CaseResponse;
import com.customer.support.ai.appserver.dto.CaseUpdateRequest;
import com.customer.support.ai.appserver.dto.CursorPage;
import com.customer.support.ai.appserver.service.CaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
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

@RestController
@RequestMapping("/api/v1/cases")
@Tag(name = "Cases", description = "Investigation case management")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "List cases with cursor pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cases page returned"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    public CursorPage<CaseResponse> list(@Parameter(description = "Pagination cursor") @RequestParam(required = false) String cursor,
                                         @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return caseService.list(cursor, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Get case detail by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Case returned"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Case not found")
    })
    public CaseDetailResponse get(@Parameter(description = "Case id") @PathVariable UUID id) {
        return caseService.getById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Update a case")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Case updated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Case not found")
    })
    public CaseResponse update(@Parameter(description = "Case id") @PathVariable UUID id, @Valid @RequestBody CaseUpdateRequest request) {
        return caseService.update(id, request);
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('SUPERVISOR')")
    @Operation(summary = "Close a case")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Case closed"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Case not found")
    })
    public CaseResponse close(@Parameter(description = "Case id") @PathVariable UUID id,
                              @Valid @RequestBody CaseCloseRequest request,
                              Authentication authentication) {
        return caseService.close(id, request, authentication.getName());
    }
}
