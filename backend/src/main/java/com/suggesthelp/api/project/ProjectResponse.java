package com.suggesthelp.api.project;

import java.time.OffsetDateTime;

public record ProjectResponse(
        Long id,
        String name,
        String clientOrg,
        Long budget,
        String status,
        OffsetDateTime createdAt
) {
}
