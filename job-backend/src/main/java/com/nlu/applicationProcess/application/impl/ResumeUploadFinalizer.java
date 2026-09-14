package com.nlu.applicationProcess.application.impl;

import com.nlu.applicationProcess.domain.model.ClaimResult;
import com.nlu.applicationProcess.domain.model.Resume;
import com.nlu.applicationProcess.domain.model.ResumeUploadSession;
import com.nlu.applicationProcess.domain.model.ResumeUploadSessionStatus;
import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.applicationProcess.domain.repository.ResumeUploadSessionRepository;
import com.nlu.identity.domain.model.User;
import com.nlu.shared.domain.exception.BadRequestException;
import com.nlu.shared.domain.exception.ForbiddenException;
import com.nlu.shared.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Transactional component managing concurrency and state transitions for ResumeUploadSession.
 * Enforces pessimistic locking, ownership checks under lock, processing tokens,
 * and business invariants (REJECTED must never have a Resume; COMPLETED must have a Resume).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeUploadFinalizer {

    private final ResumeUploadSessionRepository sessionRepository;
    private final ResumeRepository resumeRepository;

    private static final int FINALIZING_LEASE_SECONDS = 120;

    /**
     * Claims a session for finalization in a short database transaction.
     * Transitions status from PENDING_UPLOAD to FINALIZING with a unique processing token.
     */
    @Transactional
    public ClaimResult claimForFinalizing(UUID uploadId, User currentUser) {
        ResumeUploadSession session = sessionRepository.findByIdForUpdate(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("Upload session not found: " + uploadId));

        // Re-check ownership under lock
        if (session.getUserId() != currentUser.getId()) {
            log.warn("User {} tried to complete session {} belonging to user {}",
                    currentUser.getId(), uploadId, session.getUserId());
            throw new ForbiddenException("You do not have permission to access this upload session.");
        }

        // Check already COMPLETED
        if (session.getStatus() == ResumeUploadSessionStatus.COMPLETED) {
            log.info("Session {} is already COMPLETED. Returning existing resume id: {}", uploadId, session.getResumeId());
            return ClaimResult.alreadyCompleted(session.getResumeId());
        }

        // Check REJECTED
        if (session.getStatus() == ResumeUploadSessionStatus.REJECTED) {
            throw new BadRequestException("Upload session was rejected: " + uploadId);
        }

        LocalDateTime now = LocalDateTime.now();

        // Check EXPIRED
        if (session.getStatus() == ResumeUploadSessionStatus.EXPIRED || now.isAfter(session.getExpiresAt())) {
            if (session.getStatus() != ResumeUploadSessionStatus.EXPIRED) {
                session.setStatus(ResumeUploadSessionStatus.EXPIRED);
                sessionRepository.save(session);
            }
            throw new BadRequestException("Upload session has expired: " + uploadId);
        }

        // Check active FINALIZING lease
        if (session.getStatus() == ResumeUploadSessionStatus.FINALIZING) {
            boolean leaseActive = session.getFinalizingStartedAt() != null &&
                    now.isBefore(session.getFinalizingStartedAt().plusSeconds(FINALIZING_LEASE_SECONDS));
            if (leaseActive) {
                log.info("Session {} is currently FINALIZING by another worker. Returning inProgress.", uploadId);
                return ClaimResult.inProgress();
            }
            log.warn("Session {} had expired FINALIZING lease. Taking over.", uploadId);
        }

        // Claim session: PENDING_UPLOAD (or expired FINALIZING lease) -> FINALIZING
        String processingToken = UUID.randomUUID().toString();
        session.setStatus(ResumeUploadSessionStatus.FINALIZING);
        session.setProcessingToken(processingToken);
        session.setFinalizingStartedAt(now);
        sessionRepository.save(session);

        log.info("Claimed upload session {} for FINALIZING with token: {}", uploadId, processingToken);
        return ClaimResult.claimed(processingToken, session);
    }

    /**
     * Finalizes upload in a short database transaction.
     * Reads permanentKey and originalFileName directly from the locked session,
     * creates exactly one Resume entity, and transitions status to COMPLETED.
     */
    @Transactional
    public Resume finalizeUpload(UUID uploadId, String processingToken, User currentUser) {
        ResumeUploadSession session = sessionRepository.findByIdForUpdate(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("Upload session not found: " + uploadId));

        // Re-check ownership under lock
        if (session.getUserId() != currentUser.getId()) {
            throw new ForbiddenException("You do not have permission to access this upload session.");
        }

        // Idempotency: if already COMPLETED, return the existing Resume
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

        // Verify valid state transition and processing token
        if (session.getStatus() != ResumeUploadSessionStatus.FINALIZING) {
            throw new BadRequestException("Session is not in FINALIZING state: " + uploadId);
        }

        if (processingToken != null && !Objects.equals(session.getProcessingToken(), processingToken)) {
            log.warn("Processing token mismatch for session {}. Expected: {}, Provided: {}",
                    uploadId, session.getProcessingToken(), processingToken);
            throw new BadRequestException("Invalid or expired processing token for upload session: " + uploadId);
        }

        // Create exactly one Resume entity from the locked session data
        Resume resume = new Resume();
        resume.setUser(currentUser);
        resume.setKeyCf(session.getPermanentKey());
        resume.setFileName(session.getOriginalFileName());
        resume.setRawText(null);
        resume.markUploaded();

        Resume savedResume = resumeRepository.save(resume);

        // Update session to COMPLETED
        session.setResumeId(savedResume.getId());
        session.setStatus(ResumeUploadSessionStatus.COMPLETED);
        session.setProcessingToken(null);
        sessionRepository.save(session);

        log.info("Finalized upload session {} -> created resume id: {} with status: {}",
                uploadId, savedResume.getId(), savedResume.getStatus());

        return savedResume;
    }

    /**
     * Marks session as REJECTED under pessimistic write lock.
     * Enforces invariant: resume_id MUST remain null, Resume is NEVER created.
     */
    @Transactional
    public void markRejected(UUID uploadId, String processingToken, String rejectionCode,
                             String rejectionDetail, Long actualSize, String actualContentType,
                             String actualEtag) {
        sessionRepository.findByIdForUpdate(uploadId).ifPresent(session -> {
            if (session.getStatus() == ResumeUploadSessionStatus.COMPLETED) {
                log.error("CRITICAL: Attempted to reject an already COMPLETED session id: {}. Invariant preserved.", uploadId);
                return;
            }

            if (processingToken != null && session.getProcessingToken() != null &&
                    !Objects.equals(session.getProcessingToken(), processingToken)) {
                log.warn("Processing token mismatch when rejecting session id: {}. Skipping.", uploadId);
                return;
            }

            session.setStatus(ResumeUploadSessionStatus.REJECTED);
            session.setResumeId(null); // Explicit invariant guarantee: REJECTED -> resume_id must be null
            session.setRejectionCode(rejectionCode);
            session.setRejectionDetail(rejectionDetail);
            session.setRejectedAt(LocalDateTime.now());
            session.setActualSize(actualSize);
            session.setActualContentType(actualContentType);
            session.setActualEtag(actualEtag);
            session.setProcessingToken(null);
            sessionRepository.save(session);

            log.info("Marked upload session {} as REJECTED. Code: {}, Detail: {}", uploadId, rejectionCode, rejectionDetail);
        });
    }

    /**
     * Recovers a stuck FINALIZING session. Called by the recovery cron.
     * If already has Resume or valid permanentKey, can complete; otherwise transitions to REJECTED.
     */
    @Transactional
    public boolean recoverStuckSession(UUID uploadId, boolean permanentObjectValid, User user) {
        return sessionRepository.findByIdForUpdate(uploadId).map(session -> {
            if (session.getStatus() != ResumeUploadSessionStatus.FINALIZING) {
                return false;
            }

            if (permanentObjectValid && user != null) {
                // Recover to COMPLETED
                Resume resume = new Resume();
                resume.setUser(user);
                resume.setKeyCf(session.getPermanentKey());
                resume.setFileName(session.getOriginalFileName());
                resume.setRawText(null);
                resume.markUploaded();
                Resume savedResume = resumeRepository.save(resume);

                session.setResumeId(savedResume.getId());
                session.setStatus(ResumeUploadSessionStatus.COMPLETED);
                session.setProcessingToken(null);
                sessionRepository.save(session);
                log.info("Recovered stuck session {} to COMPLETED with resumeId: {}", uploadId, savedResume.getId());
                return true;
            } else {
                // Recover to REJECTED
                session.setStatus(ResumeUploadSessionStatus.REJECTED);
                session.setResumeId(null);
                session.setRejectionCode("FINALIZING_LEASE_EXPIRED");
                session.setRejectionDetail("Session lease expired while in FINALIZING state and could not be completed.");
                session.setRejectedAt(LocalDateTime.now());
                session.setProcessingToken(null);
                sessionRepository.save(session);
                log.info("Recovered stuck session {} to REJECTED", uploadId);
                return true;
            }
        }).orElse(false);
    }
}
