package com.nlu.shared.application.impl;

import com.nlu.shared.application.CloudStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.nlu.shared.domain.exception.StorageException;
import com.nlu.shared.domain.model.StorageObjectMetadata;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import com.nlu.shared.domain.exception.ResourceNotFoundException;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudStorageServiceImpl implements CloudStorageService {

    private final S3Client s3Client;

    @Value("${cloudflare.r2.bucket-name}")
    private String bucketName;

    @Override
    public void uploadFile(byte[] data, String key, String originalName) {
        log.info("Uploading file to cloud storage — key: {}, size: {} bytes", key, data.length);

        String contentType = determineContentType(originalName);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(data));

        log.info("File uploaded to cloud storage — key: {}", key);
    }

    @Override
    public Optional<StorageObjectMetadata> headObject(String key) {
        log.debug("Fetching metadata for cloud storage key: {}", key);
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(request);
            long contentLength = response.contentLength() != null ? response.contentLength() : 0L;
            return Optional.of(new StorageObjectMetadata(
                    key,
                    contentLength,
                    response.contentType(),
                    response.eTag()
            ));
        } catch (NoSuchKeyException e) {
            log.info("Object not found in cloud storage — key: {}", key);
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                log.info("Object not found in cloud storage (404) — key: {}", key);
                return Optional.empty();
            }
            log.error("S3 error fetching metadata for key: {}, status: {}", key, e.statusCode(), e);
            throw new StorageException("Failed to retrieve object metadata from storage: " + key, e);
        } catch (Exception e) {
            log.error("Storage connection error fetching metadata for key: {}", key, e);
            throw new StorageException("Storage connection error for key: " + key, e);
        }
    }

    @Override
    public void copyObject(String sourceKey, String destinationKey) {
        log.info("Copying object server-side — from: {} to: {}", sourceKey, destinationKey);
        try {
            String copySource = SdkHttpUtils.urlEncode(bucketName) + "/" + SdkHttpUtils.urlEncodeIgnoreSlashes(sourceKey);

            CopyObjectRequest request = CopyObjectRequest.builder()
                    .copySource(copySource)
                    .destinationBucket(bucketName)
                    .destinationKey(destinationKey)
                    .build();

            s3Client.copyObject(request);
            log.info("Successfully copied object server-side — from: {} to: {}", sourceKey, destinationKey);
        } catch (Exception e) {
            log.error("Failed server-side copy — from: {} to: {}", sourceKey, destinationKey, e);
            throw new StorageException("Failed to copy object from " + sourceKey + " to " + destinationKey, e);
        }
    }

    @Override
    public void deleteObject(String key) {
        log.info("Deleting object from cloud storage — key: {}", key);
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
            log.info("Successfully deleted object from cloud storage — key: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete object from cloud storage — key: {}", key, e);
            throw new StorageException("Failed to delete object: " + key, e);
        }
    }

    @Override
    public byte[] getObjectBytes(String key) {
        log.info("Fetching object bytes from cloud storage — key: {}", key);
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            return s3Client.getObject(request).readAllBytes();
        } catch (NoSuchKeyException e) {
            log.warn("Object not found in cloud storage — key: {}", key);
            throw new ResourceNotFoundException("Object not found in storage: " + key);
        } catch (Exception e) {
            log.error("Failed to fetch object bytes from cloud storage — key: {}", key, e);
            throw new StorageException("Failed to fetch object from storage: " + key, e);
        }
    }

    private String determineContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";

        String lowerCaseName = fileName.toLowerCase();
        if (lowerCaseName.endsWith(".pdf")) return "application/pdf";
        if (lowerCaseName.endsWith(".doc")) return "application/msword";
        if (lowerCaseName.endsWith(".docx"))
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lowerCaseName.endsWith(".png")) return "image/png";
        if (lowerCaseName.endsWith(".jpg") || lowerCaseName.endsWith(".jpeg")) return "image/jpeg";

        return "application/octet-stream";
    }
}

