package com.nlu.applicationProcess.infrastructure.job;

import com.nlu.applicationProcess.application.impl.ResumeUploadFinalizer;
import com.nlu.applicationProcess.application.impl.ResumeUploadSessionCleaner;
import com.nlu.applicationProcess.domain.model.ResumeUploadSession;
import com.nlu.applicationProcess.domain.model.ResumeUploadSessionStatus;
import com.nlu.applicationProcess.domain.repository.ResumeUploadSessionRepository;
import com.nlu.identity.domain.model.User;
import com.nlu.identity.domain.repository.UserRepository;
import com.nlu.shared.application.CloudStorageService;
import com.nlu.shared.domain.model.StorageObjectMetadata;
import com.nlu.applicationProcess.application.impl.ResumeUploadServiceImpl;
import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.shared.infrastructure.config.ResumeUploadProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Scheduled job responsible for cleaning up abandoned sessions, retrying failed storage deletions,
 * cleaning up rejected sessions while preserving audit rows, and recovering stuck FINALIZING sessions.
 * Multi-instance safe via pessimistic locking and state transitions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeUploadCleanupJob {

    private final ResumeUploadSessionRepository sessionRepository;
    private final ResumeRepository resumeRepository;
    private final CloudStorageService cloudStorageService;
    private final ResumeUploadSessionCleaner sessionCleaner;
    private final ResumeUploadFinalizer finalizer;
    private final UserRepository userRepository;
    private final ResumeUploadProperties properties;

    private static final int FINALIZING_LEASE_TIMEOUT_MINUTES = 5;

    @Scheduled(fixedDelayString = "${app.resume-upload.cleanup-interval-ms:300000}")
    public void cleanupExpiredSessions() {
        cleanupAbandonedSessions();
        retryFailedExpiredCleanups();
        cleanupRejectedSessions();
        recoverStuckFinalizingSessions();
    }

    /**
     * 1. PENDING_UPLOAD abandoned:
     * Mark EXPIRED under lock, delete temp object.
     * If storage delete succeeds -> delete DB row.
     * If storage delete fails -> keep DB row, record failure attempt.
     */
    public void cleanupAbandonedSessions() {
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
                    // Mark as EXPIRED under lock in a short isolated transaction
                    boolean marked = sessionCleaner.markExpired(session.getId());
                    if (marked) {
                        // Delete temp object in storage outside transaction
                        try {
                            cloudStorageService.deleteObject(session.getTempKey());
                            log.info("Cleaned up temp storage object for session: {}", session.getId());
                            // Delete DB row ONLY if storage cleanup succeeded
                            sessionCleaner.deleteExpiredSession(session.getId());
                        } catch (Exception storageEx) {
                            log.warn("Failed to delete temp storage for session {}: {}. Preserving DB row.",
                                    session.getId(), storageEx.getMessage());
                            sessionCleaner.recordCleanupFailure(session.getId(), storageEx.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to process cleanup for session: {}", session.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error during expired resume upload sessions cleanup batch", e);
        }
    }

    /**
     * Retry storage cleanup for EXPIRED sessions that previously failed.
     */
    public void retryFailedExpiredCleanups() {
        try {
            List<ResumeUploadSession> failedSessions = sessionRepository.findByStatusAndCleanupCompletedAtIsNull(
                    ResumeUploadSessionStatus.EXPIRED,
                    PageRequest.of(0, properties.getCleanupBatchSize())
            );

            for (ResumeUploadSession session : failedSessions) {
                if (session.getCleanupAttempts() >= 10) {
                    continue;
                }
                try {
                    cloudStorageService.deleteObject(session.getTempKey());
                    log.info("Successfully deleted temp storage on retry for session: {}", session.getId());
                    sessionCleaner.deleteExpiredSession(session.getId());
                } catch (Exception storageEx) {
                    sessionCleaner.recordCleanupFailure(session.getId(), storageEx.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error retrying failed EXPIRED cleanups", e);
        }
    }

    /**
     * 2. REJECTED:
     * Delete temp object and permanent orphan if any.
     * KEEP database row REJECTED for audit/traceability.
     * Verifies session has no resumeId and key is not referenced before deleting permanentKey.
     * Records failure and retries if either deletion fails; only marks success when both are cleaned.
     */
    public void cleanupRejectedSessions() {
        try {
            List<ResumeUploadSession> rejectedSessions = sessionRepository.findByStatusAndCleanupCompletedAtIsNull(
                    ResumeUploadSessionStatus.REJECTED,
                    PageRequest.of(0, properties.getCleanupBatchSize())
            );

            for (ResumeUploadSession session : rejectedSessions) {
                if (session.getCleanupAttempts() >= 10) {
                    continue;
                }
                try {
                    boolean tempCleaned = cleanupSingleKey(session.getTempKey());

                    boolean permCleaned = true;
                    String permKey = session.getPermanentKey();
                    if (permKey != null && !permKey.isBlank()) {
                        if (session.getResumeId() != null) {
                            log.error("Invariant violation: REJECTED session {} has non-null resumeId {}", session.getId(), session.getResumeId());
                            permCleaned = false;
                        } else if (resumeRepository.existsByKeyCf(permKey)) {
                            log.warn("Permanent key {} is referenced by an existing Resume. Skipping permanent key deletion for session {}.",
                                    permKey, session.getId());
                            permCleaned = true;
                        } else {
                            permCleaned = cleanupSingleKey(permKey);
                        }
                    }

                    if (tempCleaned && permCleaned) {
                        sessionCleaner.recordCleanupSuccess(session.getId());
                        log.info("Cleaned up storage for REJECTED session {} while preserving DB row for audit", session.getId());
                    } else {
                        String errMsg = String.format("Storage cleanup incomplete for session %s: tempCleaned=%s, permCleaned=%s",
                                session.getId(), tempCleaned, permCleaned);
                        sessionCleaner.recordCleanupFailure(session.getId(), errMsg);
                    }
                } catch (Exception e) {
                    log.warn("Error cleaning storage for REJECTED session {}: {}", session.getId(), e.getMessage());
                    sessionCleaner.recordCleanupFailure(session.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error cleaning REJECTED sessions batch", e);
        }
    }

    /**
     * 3. FINALIZING stuck recovery:
     * Recovers sessions stuck in FINALIZING longer than lease timeout.
     * Verifies existence, positive size, size match, content type allowlist, and declared content type match.
     */
    public void recoverStuckFinalizingSessions() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(FINALIZING_LEASE_TIMEOUT_MINUTES);
            List<ResumeUploadSession> stuckSessions = sessionRepository.findByStatusAndFinalizingStartedAtBefore(
                    ResumeUploadSessionStatus.FINALIZING,
                    threshold,
                    PageRequest.of(0, properties.getCleanupBatchSize())
            );

            for (ResumeUploadSession session : stuckSessions) {
                try {
                    Optional<StorageObjectMetadata> permMetaOpt = cloudStorageService.headObject(session.getPermanentKey());
                    boolean permanentValid = permMetaOpt.isPresent() &&
                            permMetaOpt.get().contentLength() > 0 &&
                            permMetaOpt.get().contentLength() == session.getDeclaredSize() &&
                            ResumeUploadServiceImpl.ALLOWED_CONTENT_TYPES.contains(ResumeUploadServiceImpl.normalizeContentType(permMetaOpt.get().contentType())) &&
                            ResumeUploadServiceImpl.isMatchingContentType(permMetaOpt.get().contentType(), session.getDeclaredContentType());

                    User user = null;
                    if (permanentValid) {
                        user = userRepository.findById(session.getUserId()).orElse(null);
                    }

                    boolean recovered = finalizer.recoverStuckSession(session.getId(), permanentValid && user != null, user);
                    if (recovered) {
                        if (!permanentValid || user == null) {
                            // Clean up storage orphans
                            deleteObjectSilently(session.getTempKey());
                            deleteObjectSilently(session.getPermanentKey());
                        }
                    }
                } catch (Exception e) {
                    log.error("Error recovering stuck session: {}", session.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error during stuck FINALIZING sessions recovery", e);
        }
    }

    private boolean cleanupSingleKey(String key) {
        if (key == null || key.isBlank()) {
            return true;
        }
        try {
            cloudStorageService.deleteObject(key);
            return true;
        } catch (Exception deleteEx) {
            try {
                if (cloudStorageService.headObject(key).isEmpty()) {
                    return true;
                }
            } catch (Exception headEx) {
                log.debug("HEAD failed after delete failed for key {}: {}", key, headEx.getMessage());
            }
            log.warn("Failed to delete object in storage for key {}: {}", key, deleteEx.getMessage());
            return false;
        }
    }

    private void deleteObjectSilently(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            cloudStorageService.deleteObject(key);
        } catch (Exception e) {
            log.warn("Silently ignored error deleting object: {}: {}", key, e.getMessage());
        }
    }
}
