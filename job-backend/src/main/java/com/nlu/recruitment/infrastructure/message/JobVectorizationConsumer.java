package com.nlu.recruitment.infrastructure.message;

import com.nlu.applicationProcess.application.VectorizationClient;
import com.nlu.recruitment.domain.repository.JobRepository;
import com.nlu.shared.api.message.dto.ApiMessage;
import com.nlu.shared.application.SseEmitterService;
import com.nlu.shared.domain.model.SseMessagePayload;
import com.nlu.shared.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobVectorizationConsumer {

    private final VectorizationClient vectorizationClient;
    private final JobRepository jobRepository;
    private final SseEmitterService sseEmitterService;

    @RabbitListener(queues = RabbitMQConfig.API_QUEUE_V2)
    public void processApiService(@Payload ApiMessage message) {
        Long jobId = message.getVectorizeJdRequest() != null ? message.getVectorizeJdRequest().getJobId() : null;
        Long userId = message.getVectorizeJdRequest() != null ? message.getVectorizeJdRequest().getUserId() : null;

        try {
            vectorizationClient.vectorizeJd(message.getVectorizeJdRequest());

            // Cập nhật trạng thái isAnalyzed trên Job
            if (jobId != null) {
                jobRepository.findById(jobId).ifPresent(job -> {
                    job.markAnalyzed();
                    jobRepository.save(job);
                });
            }

            // SSE event thông báo hoàn tất
            if (userId != null && jobId != null) {
                sseEmitterService.sendEvent(userId, "job-process",
                    SseMessagePayload.builder()
                        .id(jobId)
                        .status("analyzed")
                        .message("Job analysis complete")
                        .build());
            }

        } catch (Exception e) {
            log.error("Failed to vectorize JD for job: {}", jobId, e);
            if (userId != null && jobId != null) {
                try {
                    sseEmitterService.sendEvent(userId, "job-process",
                        SseMessagePayload.builder()
                            .id(jobId)
                            .status("failed")
                            .message("Job analysis failed")
                            .build());
                } catch (Exception sseEx) {
                    log.warn("Failed to send failure SSE event for job: {}", jobId, sseEx);
                }
            }
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException("Job vectorization failed for job: " + jobId, e);
        }
    }
}
