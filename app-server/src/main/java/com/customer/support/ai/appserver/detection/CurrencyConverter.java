package com.customer.support.ai.appserver.detection;

import java.math.BigDecimal;

public interface CurrencyConverter {

    ConversionResult toInr(String currency, BigDecimal amount);

    record ConversionResult(BigDecimal amountInr, BigDecimal rateUsed) {
    }
}
