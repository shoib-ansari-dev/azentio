package com.customer.support.ai.appserver.dto;

import java.util.List;

public record CaseDetailResponse(
        CaseResponse caseInfo,
        List<AlertResponse> alerts) {
}
