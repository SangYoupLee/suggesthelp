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
                INSERT INTO documents(project_id, user_id, file_name, content_type, object_key, sha256, status, created_at, updated_at)
                VALUES (?, NULL, ?, ?, ?, ?, 'UPLOADED', NOW(), NOW())
                RETURNING id
                """, Long.class, projectId, file.getOriginalFilename(), file.getContentType(), objectKey, sha256);

        return new DocumentUploadResponse(id, projectId, file.getOriginalFilename(), objectKey, sha256, "UPLOADED", OffsetDateTime.now(ZoneOffset.UTC));
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
