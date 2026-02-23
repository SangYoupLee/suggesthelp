package com.suggesthelp.api.project;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreateProjectRequest(
        @NotBlank String name,
        String clientOrg,
        Long budget,
        LocalDate dueDate
) {
}
