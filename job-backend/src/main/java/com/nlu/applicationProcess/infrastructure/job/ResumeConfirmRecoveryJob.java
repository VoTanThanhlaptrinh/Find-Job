package com.nlu.applicationProcess.infrastructure.job;

import com.nlu.applicationProcess.domain.model.ResumeUploadSessionStatus;
import com.nlu.applicationProcess.domain.repository.ResumeUploadSessionRepository;
import com.nlu.applicationProcess.domain.event.ResumeConfirmRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeConfirmRecoveryJob {
    private final ResumeUploadSessionRepository sessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelayString = "${app.file-service.confirm-recovery-interval-ms:60000}")
    public void republishPendingConfirms() {
        var pendingSessions = sessionRepository.findByStatusOrderByUpdatedAtAsc(
                ResumeUploadSessionStatus.CONFIRM_PENDING,
                PageRequest.of(0, 100));

        var staleConfirmingSessions = sessionRepository.findStaleByStatus(
                ResumeUploadSessionStatus.CONFIRMING,
                LocalDateTime.now().minusMinutes(5),
                PageRequest.of(0, 100));

        var sessionsToRepublish = new java.util.ArrayList<>(pendingSessions);
        sessionsToRepublish.addAll(staleConfirmingSessions);
        for (var session : sessionsToRepublish) {
            if (session.getResumeId() != null) {
                try {
                    eventPublisher.publishEvent(new ResumeConfirmRequestedEvent(session.getResumeId()));
                } catch (Exception e) {
                    log.warn("Failed to republish pending confirmation for CV {}: {}",
                            session.getResumeId(), e.getMessage());
                }
            }
        }
    }
}
