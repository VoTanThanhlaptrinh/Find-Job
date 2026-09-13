package com.nlu.applicationProcess.infrastructure.message;

import com.nlu.applicationProcess.api.dto.client.ResumeParsingMessage;
import com.nlu.applicationProcess.api.dto.client.ResumeRequest;
import com.nlu.applicationProcess.application.ResumeParsingService;
import com.nlu.applicationProcess.application.VectorizationClient;
import com.nlu.applicationProcess.domain.repository.ResumeRepository;
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
public class ResumeParsingConsumer {

    private final ResumeParsingService resumeParsingService;
    private final VectorizationClient vectorizationClient;
    private final SseEmitterService sseEmitterService;
    private final ResumeRepository resumeRepository;

    @RabbitListener(queues = RabbitMQConfig.PARSING_QUEUE_V2)
    public void parsingRawText(@Payload ResumeParsingMessage message) {
        try {
            long parsingStartTime = System.currentTimeMillis();
            sseEmitterService.sendEvent(message.userId(), "resume-process",
                    SseMessagePayload.builder()
                            .id(message.cvId())
                            .status("parsing")
                            .message("AI is parsing your resume...")
                            .build());

            var res = resumeParsingService.processResume(message.rawText());
            log.info(res.toString());

            long parsingEndTime = System.currentTimeMillis();
            double parsingTime = Math.round((parsingEndTime - parsingStartTime) / 100.0) / 10.0;

            sseEmitterService.sendEvent(message.userId(), "resume-process",
                    SseMessagePayload.builder()
                            .id(message.cvId())
                            .status("vectorizing")
                            .message("Resume parsing complete, vectorizing data...")
                            .executionTime(parsingTime)
                            .build());

            long vectorizeStartTime = System.currentTimeMillis();
            vectorizationClient.vectorizeCv(new ResumeRequest(message.userId(), message.cvId(), res));
            long vectorizeEndTime = System.currentTimeMillis();
            double vectorizeTime = Math.round((vectorizeEndTime - vectorizeStartTime) / 100.0) / 10.0;

            // Cập nhật trạng thái isAnalyzed trên Resume
            resumeRepository.findById(message.cvId()).ifPresent(cv -> {
                cv.markAnalyzed();
                resumeRepository.save(cv);
            });

            sseEmitterService.sendEvent(message.userId(), "resume-process",
                    SseMessagePayload.builder()
                            .id(message.cvId())
                            .status("analyzed")
                            .message("Resume analysis complete")
                            .executionTime(vectorizeTime)
                            .build());

        } catch (Exception e) {
            log.error("Error processing resume: {}", message.cvId(), e);
            try {
                sseEmitterService.sendEvent(message.userId(), "resume-process", SseMessagePayload.builder()
                        .id(message.cvId())
                        .status("failed")
                        .message("Resume analysis failed")
                        .build());
            } catch (Exception sseEx) {
                log.warn("Failed to send failure SSE event for resume: {}", message.cvId(), sseEx);
            }
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException("Resume processing failed for cv: " + message.cvId(), e);
        }
    }
}
