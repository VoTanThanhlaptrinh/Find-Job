package com.job_web.service.support;

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
}
