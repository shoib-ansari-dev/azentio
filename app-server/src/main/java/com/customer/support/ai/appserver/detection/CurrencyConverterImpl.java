package com.customer.support.ai.appserver.detection;

import com.customer.support.ai.appserver.entity.ExchangeRate;
import com.customer.support.ai.appserver.exception.IngestionException;
import com.customer.support.ai.appserver.repository.ExchangeRateRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class CurrencyConverterImpl implements CurrencyConverter {

    private final ExchangeRateRepository exchangeRateRepository;

    public CurrencyConverterImpl(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    @Override
    public ConversionResult toInr(String currency, BigDecimal amount) {
        ExchangeRate rate = exchangeRateRepository.findByCurrency(currency)
                .orElseThrow(() -> new IngestionException("No exchange rate for currency " + currency));
        BigDecimal rateToInr = rate.getRateToInr();
        return new ConversionResult(amount.multiply(rateToInr), rateToInr);
    }
}
