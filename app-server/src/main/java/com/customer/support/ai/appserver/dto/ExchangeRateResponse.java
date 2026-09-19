package com.customer.support.ai.appserver.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ExchangeRateResponse(
        String currency,
        BigDecimal rateToInr,
        OffsetDateTime effectiveFrom,
        String updatedBy) {
}
