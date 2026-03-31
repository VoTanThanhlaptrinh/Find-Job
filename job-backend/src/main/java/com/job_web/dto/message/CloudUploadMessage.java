package com.job_web.dto.message;

public record CloudUploadMessage(
    byte[] data,
    String key,
    String originalName
) {
}
