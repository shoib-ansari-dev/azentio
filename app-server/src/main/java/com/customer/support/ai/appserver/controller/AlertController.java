package com.customer.support.ai.appserver.controller;

import com.customer.support.ai.appserver.dto.AlertDismissRequest;
import com.customer.support.ai.appserver.dto.AlertEscalateRequest;
import com.customer.support.ai.appserver.dto.AlertResponse;
import com.customer.support.ai.appserver.dto.CursorPage;
import com.customer.support.ai.appserver.service.AlertService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "AML alert triage and lifecycle")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "List alerts with cursor pagination")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alerts page returned"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    public CursorPage<AlertResponse> list(@Parameter(description = "Filter by status") @RequestParam(required = false) String status,
                                          @Parameter(description = "Pagination cursor") @RequestParam(required = false) String cursor,
                                          @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return alertService.list(status, cursor, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Get an alert by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alert returned"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Alert not found")
    })
    public AlertResponse get(@Parameter(description = "Alert id") @PathVariable UUID id) {
        return alertService.getById(id);
    }

    @PutMapping("/{id}/acknowledge")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Acknowledge an alert")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alert acknowledged"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Alert not found")
    })
    public AlertResponse acknowledge(@Parameter(description = "Alert id") @PathVariable UUID id) {
        return alertService.acknowledge(id);
    }

    @PutMapping("/{id}/dismiss")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Dismiss an alert with a reason")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alert dismissed"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Alert not found")
    })
    public AlertResponse dismiss(@Parameter(description = "Alert id") @PathVariable UUID id, @Valid @RequestBody AlertDismissRequest request) {
        return alertService.dismiss(id, request);
    }

    @PutMapping("/{id}/escalate")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Escalate an alert to a case")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alert escalated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Alert not found")
    })
    public AlertResponse escalate(@Parameter(description = "Alert id") @PathVariable UUID id,
                                  @RequestBody AlertEscalateRequest request,
                                  Authentication authentication) {
        return alertService.escalate(id, request, authentication.getName());
    }
}
