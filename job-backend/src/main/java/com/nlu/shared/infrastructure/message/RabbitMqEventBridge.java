package com.nlu.shared.infrastructure.message;

import com.nlu.applicationProcess.domain.event.ResumeAnalysisRequestedEvent;
import com.nlu.recruitment.domain.event.JobAnalysisRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridge listening to internal Spring Application Events after transaction commit
 * and publishing corresponding messages to RabbitMQ.
 *
 * Note: Publishing to RabbitMQ AFTER_COMMIT is best-effort (no Transactional Outbox pattern).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqEventBridge {

    private final MessageProducer messageProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleJobAnalysisRequested(JobAnalysisRequestedEvent event) {
        log.info("Transaction committed. Forwarding JobAnalysisRequestedEvent to RabbitMQ for job: {}",
                event.request() != null ? event.request().getJobId() : null);
        messageProducer.processJdVectorize(event.request());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleResumeAnalysisRequested(ResumeAnalysisRequestedEvent event) {
        log.info("Transaction committed. Forwarding ResumeAnalysisRequestedEvent to RabbitMQ for CV: {}",
                event.message() != null ? event.message().cvId() : null);
        messageProducer.processAI(event.message());
    }
}
