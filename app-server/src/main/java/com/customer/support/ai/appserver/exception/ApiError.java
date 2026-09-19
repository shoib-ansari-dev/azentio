package com.customer.support.ai.appserver.exception;

import java.time.OffsetDateTime;

public record ApiError(int status, String code, String message, OffsetDateTime timestamp) {

    public static ApiError of(int status, String code, String message) {
        return new ApiError(status, code, message, OffsetDateTime.now());
    }
}
