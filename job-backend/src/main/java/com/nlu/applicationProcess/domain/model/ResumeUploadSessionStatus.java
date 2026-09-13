package com.nlu.applicationProcess.domain.model;

public enum ResumeUploadSessionStatus {
    /**
     * Presigned URL đã được cấp, đang chờ client upload và gọi complete.
     */
    PENDING_UPLOAD,

    /**
     * File upload hợp lệ, object đã được copy sang resumes/ và Resume entity đã được tạo.
     */
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
