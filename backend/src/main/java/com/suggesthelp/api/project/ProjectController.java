package com.suggesthelp.api.project;

import com.suggesthelp.api.common.ApiResponse;
import jakarta.validation.Valid;
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

    @PostMapping
    public ApiResponse<ProjectResponse> createProject(@RequestBody @Valid CreateProjectRequest request) {
        return ApiResponse.ok(projectService.createProject(request));
    }

    @PostMapping("/{id}/upload")
    public ApiResponse<DocumentUploadResponse> upload(
            @PathVariable("id") @Positive Long projectId,
            @RequestPart("file") MultipartFile file
    ) throws Exception {
        return ApiResponse.ok(projectService.uploadDocument(projectId, file));
    }

    @GetMapping("/{id}/status")
    public ApiResponse<ProjectStatusResponse> status(@PathVariable("id") @Positive Long projectId) {
        return ApiResponse.ok(projectService.getProjectStatus(projectId));
    }
}
