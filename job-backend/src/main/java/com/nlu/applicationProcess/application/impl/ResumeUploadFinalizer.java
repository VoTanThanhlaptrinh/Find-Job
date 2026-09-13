package com.nlu.applicationProcess.application.impl;

import com.nlu.applicationProcess.domain.model.Resume;
import com.nlu.applicationProcess.domain.model.ResumeUploadSession;
import com.nlu.applicationProcess.domain.model.ResumeUploadSessionStatus;
import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.applicationProcess.domain.repository.ResumeUploadSessionRepository;
import com.nlu.identity.domain.model.User;
import com.nlu.shared.domain.exception.BadRequestException;
import com.nlu.shared.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Dedicated transactional bean executing the short database transaction to finalize
 * the resume upload session, lock the session record with PESSIMISTIC_WRITE,
 * and create exactly one Resume entity.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeUploadFinalizer {

    private final ResumeUploadSessionRepository sessionRepository;
    private final ResumeRepository resumeRepository;

    @Transactional
    public Resume finalizeUpload(UUID uploadId, User user, String permanentKey, String originalFileName) {
        // Acquire pessimistic write lock on the upload session
        ResumeUploadSession session = sessionRepository.findByIdForUpdate(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("Upload session not found: " + uploadId));

        // Re-check idempotency under lock
        if (session.getStatus() == ResumeUploadSessionStatus.COMPLETED) {
            log.info("Session {} is already COMPLETED. Returning existing resume id: {}", uploadId, session.getResumeId());
            if (session.getResumeId() != null) {
                return resumeRepository.findById(session.getResumeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Associated resume not found: " + session.getResumeId()));
            }
        }

        if (session.getStatus() == ResumeUploadSessionStatus.EXPIRED) {
            throw new BadRequestException("Upload session has expired: " + uploadId);
        }

        if (session.getStatus() == ResumeUploadSessionStatus.REJECTED) {
            throw new BadRequestException("Upload session was rejected: " + uploadId);
        }

        // Create exactly one Resume entity
        Resume resume = new Resume();
        resume.setUser(user);
        resume.setKeyCf(permanentKey);
        resume.setFileName(originalFileName);
        resume.setRawText(null);
        resume.markUploaded();

        Resume savedResume = resumeRepository.save(resume);

        // Update session
        session.setResumeId(savedResume.getId());
        session.setStatus(ResumeUploadSessionStatus.COMPLETED);
        sessionRepository.save(session);

        log.info("Finalized upload session {} -> created resume id: {} with status: {}",
                uploadId, savedResume.getId(), savedResume.getStatus());

        return savedResume;
    }
}
