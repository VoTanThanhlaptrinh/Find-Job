package com.nlu.applicationProcess.infrastructure.message;

import com.nlu.applicationProcess.api.dto.client.ResumeConfirmMessage;
import com.nlu.applicationProcess.domain.model.Resume;
import com.nlu.applicationProcess.domain.model.ResumeStorageStatus;
import com.nlu.applicationProcess.domain.model.ResumeUploadSession;
import com.nlu.applicationProcess.domain.model.ResumeUploadSessionStatus;
import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.applicationProcess.domain.repository.ResumeUploadSessionRepository;
import com.nlu.shared.application.FileServiceClient;
import com.nlu.shared.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeConfirmConsumer {
    private static final int MAX_ATTEMPTS = 3;

    private final FileServiceClient fileServiceClient;
    private final ResumeRepository resumeRepository;
    private final ResumeUploadSessionRepository sessionRepository;

    @RabbitListener(queues = RabbitMQConfig.RESUME_CONFIRM_QUEUE)
    @Transactional
    public void confirmResume(ResumeConfirmMessage message) {
        // Serialize duplicate deliveries per CV. Without the row lock, a slow failing
        // delivery could overwrite a successful concurrent delivery as CONFIRM_FAILED.
        ResumeUploadSession session = sessionRepository.findByResumeIdForUpdate(message.cvId())
                .orElseThrow(() -> new IllegalStateException("Upload session not found for CV " + message.cvId()));
        Resume resume = resumeRepository.findById(message.cvId())
                .orElseThrow(() -> new IllegalStateException("CV not found: " + message.cvId()));

        if (session.getStatus() == ResumeUploadSessionStatus.COMPLETED
                || resume.getStorageStatus() == ResumeStorageStatus.CONFIRMED) {
            if (resume.getStorageStatus() == ResumeStorageStatus.CONFIRMED
                    && session.getStatus() != ResumeUploadSessionStatus.COMPLETED) {
                session.setStatus(ResumeUploadSessionStatus.COMPLETED);
                sessionRepository.save(session);
            }
            log.info("CV {} is already confirmed; skipping duplicate message", message.cvId());
            return;
        }
        if (session.getStatus() == ResumeUploadSessionStatus.CONFIRM_FAILED
                || resume.getStorageStatus() == ResumeStorageStatus.CONFIRM_FAILED) {
            log.info("CV {} has exhausted confirm attempts; awaiting operational recovery", message.cvId());
            return;
        }

        session.setStatus(ResumeUploadSessionStatus.CONFIRMING);
        sessionRepository.save(session);

        for (int attempt = Math.max(1, session.getConfirmAttempts() + 1);
                attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                var confirmed = fileServiceClient.confirm(session.getTempKey());
                if (confirmed.key() == null || confirmed.key().isBlank()
                        || !confirmed.key().equals(session.getPermanentKey())) {
                    throw new IllegalStateException("File service returned an unexpected permanent key");
                }

                resume.markStorageConfirmed(confirmed.key());
                resumeRepository.save(resume);
                session.setStatus(ResumeUploadSessionStatus.COMPLETED);
                session.setConfirmAttempts(attempt);
                session.setConfirmLastError(null);
                session.setConfirmCompletedAt(LocalDateTime.now());
                sessionRepository.save(session);
                log.info("Confirmed storage for CV {} on attempt {}", message.cvId(), attempt);
                return;
            } catch (Exception exception) {
                String detail = conciseError(exception);
                session.setConfirmAttempts(attempt);
                session.setConfirmLastError(detail);

                if (attempt == MAX_ATTEMPTS) {
                    resume.markStorageConfirmFailed(detail);
                    resumeRepository.save(resume);
                    session.setStatus(ResumeUploadSessionStatus.CONFIRM_FAILED);
                    sessionRepository.save(session);
                    log.error("File-service confirm failed for CV {} after {} attempts: {}",
                            message.cvId(), MAX_ATTEMPTS, detail);
                    return;
                }

                sessionRepository.save(session);
                log.warn("File-service confirm attempt {}/{} failed for CV {}: {}",
                        attempt, MAX_ATTEMPTS, message.cvId(), detail);
                try {
                    Thread.sleep(500L * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while retrying file-service confirm", interrupted);
                }
            }
        }
    }

    private String conciseError(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.substring(0, Math.min(message.length(), 512));
    }
}
