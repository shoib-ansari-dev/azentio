package com.customer.support.ai.appserver.controller;

import com.customer.support.ai.appserver.dto.RuleResponse;
import com.customer.support.ai.appserver.dto.RuleUpdateRequest;
import com.customer.support.ai.appserver.service.RuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rules")
@Tag(name = "Rules", description = "Detection rule configuration")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "List all detection rules")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rules returned"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    public List<RuleResponse> list() {
        return ruleService.listAll();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a detection rule")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rule updated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    public RuleResponse update(@Parameter(description = "Rule id") @PathVariable UUID id,
                               @Valid @RequestBody RuleUpdateRequest request,
                               Authentication authentication) {
        return ruleService.update(id, request, authentication.getName());
    }
}
