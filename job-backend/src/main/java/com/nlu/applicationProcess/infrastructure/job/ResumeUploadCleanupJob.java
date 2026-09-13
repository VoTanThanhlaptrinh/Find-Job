package com.nlu.applicationProcess.infrastructure.job;

import com.nlu.applicationProcess.application.impl.ResumeUploadSessionCleaner;
import com.nlu.applicationProcess.domain.model.ResumeUploadSession;
import com.nlu.applicationProcess.domain.model.ResumeUploadSessionStatus;
import com.nlu.applicationProcess.domain.repository.ResumeUploadSessionRepository;
import com.nlu.shared.application.CloudStorageService;
import com.nlu.shared.infrastructure.config.ResumeUploadProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeUploadCleanupJob {

    private final ResumeUploadSessionRepository sessionRepository;
    private final CloudStorageService cloudStorageService;
    private final ResumeUploadSessionCleaner sessionCleaner;
    private final ResumeUploadProperties properties;

    @Scheduled(fixedDelayString = "${app.resume-upload.cleanup-interval-ms:300000}")
    public void cleanupExpiredSessions() {
        try {
            List<ResumeUploadSession> expiredSessions = sessionRepository.findByStatusAndExpiresAtBefore(
                    ResumeUploadSessionStatus.PENDING_UPLOAD,
                    LocalDateTime.now(),
                    PageRequest.of(0, properties.getCleanupBatchSize())
            );

            if (expiredSessions.isEmpty()) {
                return;
            }

            log.info("Found {} expired PENDING_UPLOAD sessions for cleanup", expiredSessions.size());

            for (ResumeUploadSession session : expiredSessions) {
                try {
                    // Mark as EXPIRED in a short isolated transaction
                    boolean marked = sessionCleaner.markExpired(session.getId());
                    if (marked) {
                        // Delete temp object in storage best-effort outside the database transaction
                        try {
                            cloudStorageService.deleteObject(session.getTempKey());
                            log.info("Cleaned up temp object for expired session: {}", session.getId());
                        } catch (Exception storageEx) {
                            log.warn("Failed to delete temp storage for expired session {}: {}. R2 bucket lifecycle will handle it.",
                                    session.getId(), storageEx.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to clean up expired session: {}", session.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error during expired resume upload sessions cleanup batch", e);
        }
    }
}
