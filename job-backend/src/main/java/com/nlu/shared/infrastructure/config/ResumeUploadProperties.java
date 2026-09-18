package com.nlu.shared.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.resume-upload")
public class ResumeUploadProperties {

    /**
     * Presigned PUT URL expiration in minutes. Default: 10 minutes.
     */
    private int urlExpirationMinutes = 10;

    /**
     * Upload session expiration in minutes. Default: 30 minutes.
     */
    private int sessionExpirationMinutes = 30;

    /**
     * Maximum allowed CV file size in bytes. Default: 5 MB (5 * 1024 * 1024).
     */
    private long maxFileSizeBytes = 5L * 1024 * 1024;

    /**
     * Prefix for temporary storage keys. Default: "temp".
     */
    private String tempPrefix = "temp";

    /**
     * Prefix for permanent storage keys. Default: "resumes".
     */
    private String permanentPrefix = "resumes";

    /**
     * Cleanup interval in milliseconds for expired upload sessions. Default: 5 minutes.
     */
    private long cleanupIntervalMs = 300000L;

    /**
     * Maximum number of expired sessions processed per cleanup batch. Default: 50.
     */
    private int cleanupBatchSize = 50;
}
