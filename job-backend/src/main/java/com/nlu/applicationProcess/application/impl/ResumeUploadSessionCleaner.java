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
 * Transactional helper for marking expired sessions in short isolated transactions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeUploadSessionCleaner {

    private final ResumeUploadSessionRepository sessionRepository;

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
}
