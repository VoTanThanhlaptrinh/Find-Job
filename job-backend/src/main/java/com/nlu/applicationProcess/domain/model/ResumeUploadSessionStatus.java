package com.nlu.applicationProcess.domain.model;

public enum ResumeUploadSessionStatus {
    /**
     * Presigned URL đã được cấp, đang chờ client upload và gọi complete.
     */
    PENDING_UPLOAD,

    /** Client completed the direct upload; storage confirm is being processed. */
    FINALIZING,

    /** The storage service confirmed the object and the Resume record was created. */
    COMPLETED,

    /**
     * Upload bị từ chối do metadata object trên storage không hợp lệ (size, content-type, not found).
     */
    REJECTED,

    /**
     * Session đã hết hạn mà chưa hoàn tất upload.
     */
    EXPIRED
}
