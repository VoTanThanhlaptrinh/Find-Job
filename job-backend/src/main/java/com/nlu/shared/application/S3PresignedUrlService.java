package com.nlu.shared.application;

/**
 * Service interface để tạo Pre-signed URL cho S3/Cloudflare R2.
 */
public interface S3PresignedUrlService {

    /**
     * Tạo Pre-signed URL để xem file trực tiếp trên trình duyệt (inline).
     *
     * @param key            Key của file trên S3/R2
     * @param expirationMinutes Thời gian URL có hiệu lực (phút)
     * @return Pre-signed URL để xem file
     */
    String generateViewUrl(String key, int expirationMinutes);

    /**
     * Tạo Pre-signed URL để tải file về (attachment).
     *
     * @param key              Key của file trên S3/R2
     * @param originalFileName Tên file gốc (dùng cho Content-Disposition)
     * @param expirationMinutes Thời gian URL có hiệu lực (phút)
     * @return Pre-signed URL để tải file
     */
    String generateDownloadUrl(String key, String originalFileName, int expirationMinutes);

    /**
     * Tạo Pre-signed URL cho HTTP PUT để upload file trực tiếp lên Cloudflare R2 / S3.
     *
     * @param key               Key tạm do backend sinh
     * @param contentType       Content-Type hợp lệ của file
     * @param uploadId          UUID của upload session (gán vào object metadata nếu tương thích)
     * @param expirationMinutes Thời gian URL có hiệu lực (phút)
     * @return PresignedUploadUrlResponse chứa URL, method, requiredHeaders và expiresAt
     */
    com.nlu.shared.domain.model.PresignedUploadUrlResponse generateUploadUrl(String key, String contentType, java.util.UUID uploadId, int expirationMinutes);
}

