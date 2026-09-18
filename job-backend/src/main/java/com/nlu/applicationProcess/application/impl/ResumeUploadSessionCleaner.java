package com.nlu.applicationProcess.application.impl;

import com.nlu.applicationProcess.domain.model.ResumeUploadSession;
import com.nlu.applicationProcess.domain.model.ResumeUploadSessionStatus;
import com.nlu.applicationProcess.domain.repository.ResumeUploadSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transactional helper for marking and managing sessions during cleanup in short isolated transactions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeUploadSessionCleaner {

    private final ResumeUploadSessionRepository sessionRepository;

    /**
     * Marks an expired PENDING_UPLOAD session as EXPIRED under pessimistic write lock.
     * Never touches FINALIZING, COMPLETED, or REJECTED sessions.
     */
    @Transactional
    public boolean markExpired(UUID uploadId) {
        return sessionRepository.findByIdForUpdate(uploadId).map(session -> {
            if (session.getStatus() == ResumeUploadSessionStatus.PENDING_UPLOAD &&
                    session.getExpiresAt().isBefore(LocalDateTime.now())) {
                session.setStatus(ResumeUploadSessionStatus.EXPIRED);
                sessionRepository.save(session);
                log.info("Marked expired upload session: {}", uploadId);
                return true;
            }
            return false;
        }).orElse(false);
    }

    /**
     * Deletes an EXPIRED session row from the database after storage cleanup has succeeded.
     */
    @Transactional
    public void deleteExpiredSession(UUID uploadId) {
        sessionRepository.findByIdForUpdate(uploadId).ifPresent(session -> {
            if (session.getStatus() == ResumeUploadSessionStatus.EXPIRED) {
                sessionRepository.delete(session);
                log.info("Deleted expired session row from DB: {}", uploadId);
            }
        });
    }

    /**
     * Records a failed storage cleanup attempt on an EXPIRED session.
     * Preserves the database row so tempKey is not lost.
     */
    @Transactional
    public void recordCleanupFailure(UUID uploadId, String error) {
        sessionRepository.findByIdForUpdate(uploadId).ifPresent(session -> {
            session.setCleanupAttempts(session.getCleanupAttempts() + 1);
            String truncated = (error != null && error.length() > 500) ? error.substring(0, 500) : error;
            session.setCleanupLastError(truncated);
            sessionRepository.save(session);
            log.warn("Recorded cleanup failure for session {}: attempt #{}", uploadId, session.getCleanupAttempts());
        });
    }

    /**
     * Marks cleanup completed on a session (e.g. for REJECTED sessions).
     */
    @Transactional
    public void recordCleanupSuccess(UUID uploadId) {
        sessionRepository.findByIdForUpdate(uploadId).ifPresent(session -> {
            session.setCleanupCompletedAt(LocalDateTime.now());
            sessionRepository.save(session);
            log.info("Recorded cleanup success for session: {}", uploadId);
        });
    }
}
