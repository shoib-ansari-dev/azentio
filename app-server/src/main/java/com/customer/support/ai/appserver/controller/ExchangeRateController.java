package com.customer.support.ai.appserver.controller;

import com.customer.support.ai.appserver.dto.ExchangeRateResponse;
import com.customer.support.ai.appserver.dto.ExchangeRateUpdateRequest;
import com.customer.support.ai.appserver.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exchange-rates")
@Tag(name = "Exchange Rates", description = "Currency conversion rate configuration")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "List all exchange rates")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rates returned"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    public List<ExchangeRateResponse> list() {
        return exchangeRateService.listAll();
    }

    @PutMapping("/{currency}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an exchange rate for a currency")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rate updated"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Currency not found")
    })
    public ExchangeRateResponse update(@Parameter(description = "ISO currency code") @PathVariable String currency,
                                       @Valid @RequestBody ExchangeRateUpdateRequest request,
                                       Authentication authentication) {
        return exchangeRateService.update(currency, request, authentication.getName());
    }
}
