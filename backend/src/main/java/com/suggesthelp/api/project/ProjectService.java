package com.suggesthelp.api.project;

import io.minio.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class ProjectService {

    private final JdbcTemplate jdbcTemplate;
    private final MinioClient minioClient;
    private final com.suggesthelp.api.config.MinioProperties minioProperties;

    public ProjectService(JdbcTemplate jdbcTemplate, MinioClient minioClient, com.suggesthelp.api.config.MinioProperties minioProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO projects(user_id, created_by, name, client_org, budget, due_date, status, created_at, updated_at)
                VALUES (NULL, 'system', ?, ?, ?, ?, 'DRAFT', NOW(), NOW())
                RETURNING id
                """, Long.class, request.name(), request.clientOrg(), request.budget(), request.dueDate());

        return jdbcTemplate.queryForObject("""
                SELECT id, name, client_org, budget, status, created_at
                FROM projects
                WHERE id = ?
                """, (rs, rowNum) -> new ProjectResponse(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("client_org"),
                rs.getLong("budget"),
                rs.getString("status"),
                rs.getObject("created_at", OffsetDateTime.class)
        ), id);
    }

    @Transactional
    public DocumentUploadResponse uploadDocument(Long projectId, MultipartFile file) throws Exception {
        validateProject(projectId);

        byte[] bytes = file.getBytes();
        String sha256 = sha256(bytes);
        ensureUniqueSha256(sha256);
        ensureBucket();

        String objectKey = "projects/%d/%s-%s".formatted(projectId, UUID.randomUUID(), file.getOriginalFilename());
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(minioProperties.bucket())
                .object(objectKey)
                .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                .contentType(file.getContentType())
                .build());

        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO documents(project_id, user_id, created_by, file_name, content_type, object_key, sha256, status, created_at, updated_at)
                VALUES (?, NULL, 'system', ?, ?, ?, ?, 'uploaded', NOW(), NOW())
                RETURNING id
                """, Long.class, projectId, file.getOriginalFilename(), file.getContentType(), objectKey, sha256);

        return new DocumentUploadResponse(id, projectId, file.getOriginalFilename(), objectKey, sha256, "uploaded", OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Transactional(readOnly = true)
    public ProjectStatusResponse getProjectStatus(Long projectId) {
        validateProject(projectId);

        Long latestDocumentId = jdbcTemplate.query("""
                SELECT id FROM documents WHERE project_id = ? ORDER BY created_at DESC LIMIT 1
                """, rs -> rs.next() ? rs.getLong(1) : null, projectId);

        String latestDocumentStatus = latestDocumentId == null ? null : jdbcTemplate.queryForObject(
                "SELECT status FROM documents WHERE id = ?", String.class, latestDocumentId);

        Long latestAnalysisRunId = jdbcTemplate.query("""
                SELECT id FROM analysis_runs WHERE project_id = ? ORDER BY created_at DESC LIMIT 1
                """, rs -> rs.next() ? rs.getLong(1) : null, projectId);

        String latestAnalysisRunStatus = latestAnalysisRunId == null ? null : jdbcTemplate.queryForObject(
                "SELECT status FROM analysis_runs WHERE id = ?", String.class, latestAnalysisRunId);

        return new ProjectStatusResponse(
                projectId,
                latestDocumentId,
                latestDocumentStatus,
                latestAnalysisRunId,
                latestAnalysisRunStatus,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }

    private void validateProject(Long projectId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM projects WHERE id = ?", Integer.class, projectId);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("Project not found: " + projectId);
        }
    }

    private void ensureUniqueSha256(String sha256) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM documents WHERE sha256 = ?", Integer.class, sha256);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("Duplicate document detected by SHA-256");
        }
    }

    private String sha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.bucket()).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.bucket()).build());
        }
    }
}
