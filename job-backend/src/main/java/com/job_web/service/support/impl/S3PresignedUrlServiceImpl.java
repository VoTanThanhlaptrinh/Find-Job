package com.job_web.service.support.impl;

import com.job_web.service.support.S3PresignedUrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

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
}
