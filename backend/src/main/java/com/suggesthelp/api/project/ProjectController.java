package com.suggesthelp.api.project;

import com.suggesthelp.api.common.ApiResponse;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/projects")
@Validated
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping("/{id}/upload")
    public ApiResponse<DocumentUploadResponse> upload(
            @PathVariable("id") @Positive Long projectId,
            @RequestPart("file") MultipartFile file
    ) throws Exception {
        return ApiResponse.ok(projectService.uploadDocument(projectId, file));
    }
}
