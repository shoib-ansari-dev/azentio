package com.customer.support.ai.appserver.dto;

import java.util.List;

public record TransactionCreateResponse(
        TransactionDetailResponse transaction,
        List<AlertResponse> alerts) {
}
