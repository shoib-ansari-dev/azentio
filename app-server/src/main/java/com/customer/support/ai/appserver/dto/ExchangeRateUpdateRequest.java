package com.customer.support.ai.appserver.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ExchangeRateUpdateRequest(
        @NotNull @Positive BigDecimal rateToInr) {
}
