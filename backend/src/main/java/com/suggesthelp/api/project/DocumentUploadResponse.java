package com.suggesthelp.api.project;

import java.time.OffsetDateTime;

public record DocumentUploadResponse(
        Long documentId,
        Long projectId,
        String fileName,
        String objectKey,
        String sha256,
        String status,
        OffsetDateTime createdAt
) {
}
