package com.suggesthelp.api.common;

import java.time.OffsetDateTime;

public record ApiError(
        String code,
        String message,
        String details,
        String path,
        OffsetDateTime timestamp
) {
}
