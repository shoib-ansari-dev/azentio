package com.customer.support.ai.appserver.dto;

import java.util.List;

public record CursorPage<T>(
        List<T> content,
        String nextCursor,
        boolean hasMore,
        long totalElements) {
}
