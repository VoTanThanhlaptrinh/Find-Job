package com.nlu.applicationProcess.domain.model;

public enum ResumeUploadSessionStatus {
    /**
     * Presigned URL đã được cấp, đang chờ client upload và gọi complete.
     */
    PENDING_UPLOAD,

    /** Client completed the direct upload; confirm is waiting to be processed. */
    FINALIZING,

    /** The resume record exists and the confirm message is waiting in RabbitMQ. */
    CONFIRM_PENDING,

    /** A consumer is currently confirming the temp object. */
    CONFIRMING,

    /** Confirm exhausted its configured attempts and needs operational recovery. */
    CONFIRM_FAILED,

    /** The storage service confirmed the object and returned its permanent key. */
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
