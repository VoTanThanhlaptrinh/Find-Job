package com.nlu.shared.application.impl;

import com.nlu.shared.application.S3PresignedUrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.nlu.shared.domain.model.PresignedUploadUrlResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3PresignedUrlServiceImpl implements S3PresignedUrlService {

    private final S3Presigner s3Presigner;

    @Value("${cloudflare.r2.bucket-name}")
    private String bucketName;

    @Override
    public String generateViewUrl(String key, int expirationMinutes) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .responseContentDisposition("inline")
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Error generating view URL for key: {}", key, e);
            throw new RuntimeException("Failed to generate view URL", e);
        }
    }

    @Override
    public String generateDownloadUrl(String key, String originalFileName, int expirationMinutes) {
        try {
            String contentDisposition = String.format("attachment; filename=\"%s\"", originalFileName);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .responseContentDisposition(contentDisposition)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Error generating download URL for key: {}", key, e);
            throw new RuntimeException("Failed to generate download URL", e);
        }
    }

    @Override
    public PresignedUploadUrlResponse generateUploadUrl(String key, String contentType, UUID uploadId, int expirationMinutes) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .putObjectRequest(putObjectRequest)
                    .build();

            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            String url = presignedRequest.url().toString();

            log.info("Generated presigned PUT upload URL for key: {}, uploadId: {}, expires in: {} minutes",
                    key, uploadId, expirationMinutes);

            Map<String, String> requiredHeaders = new LinkedHashMap<>();
            if (presignedRequest.signedHeaders() != null) {
                presignedRequest.signedHeaders().forEach((headerName, values) -> {
                    if (!"host".equalsIgnoreCase(headerName)) {
                        requiredHeaders.put(headerName, String.join(", ", values));
                    }
                });
            }

            if (!requiredHeaders.containsKey("Content-Type") && !requiredHeaders.containsKey("content-type")) {
                requiredHeaders.put("Content-Type", contentType);
            }

            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);

            return new PresignedUploadUrlResponse(url, "PUT", requiredHeaders, expiresAt);
        } catch (Exception e) {
            log.error("Error generating presigned upload URL for key: {}, uploadId: {}", key, uploadId, e);
            throw new RuntimeException("Failed to generate upload URL", e);
        }
    }
}

