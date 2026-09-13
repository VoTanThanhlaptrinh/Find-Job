package com.nlu.shared.application;

import com.nlu.shared.domain.model.StorageObjectMetadata;

import java.util.Optional;

public interface CloudStorageService {
    void uploadFile(byte[] data, String key, String originalName);

    /**
     * Lấy metadata của object trên S3/R2 mà không tải nội dung (HTTP HEAD).
     *
     * @param key Key của object
     * @return Optional chứa metadata nếu object tồn tại, Optional.empty() nếu không tồn tại (404/NoSuchKey)
     */
    Optional<StorageObjectMetadata> headObject(String key);

    /**
     * Server-side copy một object từ sourceKey sang destinationKey trên S3/R2.
     * Tuyệt đối không download bytes rồi upload lại.
     *
     * @param sourceKey      Key nguồn
     * @param destinationKey Key đích
     */
    void copyObject(String sourceKey, String destinationKey);

    /**
     * Xóa object trên S3/R2.
     *
     * @param key Key của object cần xóa
     */
    void deleteObject(String key);
}

