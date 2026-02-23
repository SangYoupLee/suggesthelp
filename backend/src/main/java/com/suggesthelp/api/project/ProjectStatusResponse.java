package com.suggesthelp.api.project;

import java.time.OffsetDateTime;

public record ProjectStatusResponse(
        Long projectId,
        Long latestDocumentId,
        String latestDocumentStatus,
        Long latestAnalysisRunId,
        String latestAnalysisRunStatus,
        OffsetDateTime updatedAt
) {
}
