package com.customer.support.ai.appserver.dto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public final class CursorSupport {

    private CursorSupport() {
    }

    public static UUID decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        return UUID.fromString(raw);
    }

    public static String encode(UUID id) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(id.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static <E, T> CursorPage<T> build(
            List<E> rows, int size, long total, Function<E, UUID> idOf, Function<E, T> toDto) {
        boolean hasMore = rows.size() > size;
        List<E> page = hasMore ? rows.subList(0, size) : rows;
        String nextCursor = hasMore ? encode(idOf.apply(page.get(page.size() - 1))) : null;
        List<T> content = page.stream().map(toDto).toList();
        return new CursorPage<>(content, nextCursor, hasMore, total);
    }
}
