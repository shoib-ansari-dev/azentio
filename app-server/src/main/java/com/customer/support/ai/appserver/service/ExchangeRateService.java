package com.customer.support.ai.appserver.service;

import com.customer.support.ai.appserver.dto.ExchangeRateResponse;
import com.customer.support.ai.appserver.dto.ExchangeRateUpdateRequest;
import com.customer.support.ai.appserver.entity.ExchangeRate;
import com.customer.support.ai.appserver.exception.EntityNotFoundException;
import com.customer.support.ai.appserver.repository.ExchangeRateRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;

    public ExchangeRateService(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    public List<ExchangeRateResponse> listAll() {
        return exchangeRateRepository.findAll().stream().map(ExchangeRateService::toResponse).toList();
    }

    @Transactional
    public ExchangeRateResponse update(String currency, ExchangeRateUpdateRequest request, String actor) {
        ExchangeRate rate = exchangeRateRepository.findByCurrency(currency)
                .orElseThrow(() -> new EntityNotFoundException("Exchange rate not found"));
        rate.setRateToInr(request.rateToInr());
        rate.setEffectiveFrom(OffsetDateTime.now());
        rate.setUpdatedBy(actor);
        return toResponse(exchangeRateRepository.save(rate));
    }

    private static ExchangeRateResponse toResponse(ExchangeRate r) {
        return new ExchangeRateResponse(r.getCurrency(), r.getRateToInr(), r.getEffectiveFrom(), r.getUpdatedBy());
    }
}
