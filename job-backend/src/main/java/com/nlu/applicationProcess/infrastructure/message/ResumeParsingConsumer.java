package com.nlu.applicationProcess.infrastructure.message;

import com.nlu.applicationProcess.api.dto.client.ResumeParsingMessage;
import com.nlu.applicationProcess.api.dto.client.ResumeRequest;
import com.nlu.applicationProcess.application.ResumeParsingService;
import com.nlu.applicationProcess.application.VectorizationClient;
import com.nlu.applicationProcess.domain.model.Resume;
import com.nlu.applicationProcess.domain.model.ResumeStatus;
import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.shared.application.SseEmitterService;
import com.nlu.shared.domain.model.SseMessagePayload;
import com.nlu.shared.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
        Optional<Resume> cvOpt = resumeRepository.findById(message.cvId());
        if (cvOpt.isEmpty()) {
            log.warn("Resume {} not found for processing", message.cvId());
            return;
        }

        Resume cv = cvOpt.get();

        // Nếu Resume đã là READY, bỏ qua message và trả về thành công để tránh xử lý trùng.
        if (cv.getStatus() == ResumeStatus.READY) {
            log.info("Resume {} is already READY, skipping duplicate parsing message.", message.cvId());
            return;
        }

        // Khi bắt đầu một lần xử lý hợp lệ, đảm bảo trạng thái là ANALYZING.
        // (Xử lý trường hợp Resume đang ở UPLOADED hoặc ANALYSIS_FAILED khi Rabbit retry)
        if (cv.getStatus() != ResumeStatus.ANALYZING) {
            cv.startAnalysis();
            cv = resumeRepository.save(cv);
        }

        try {
            long parsingStartTime = System.currentTimeMillis();
            sendSseSafe(message.userId(), message.cvId(), "parsing", "AI is parsing your resume...", null);

            var res = resumeParsingService.processResume(message.rawText());
            log.info("Resume parsed: {}", res);

            long parsingEndTime = System.currentTimeMillis();
            double parsingTime = Math.round((parsingEndTime - parsingStartTime) / 100.0) / 10.0;

            sendSseSafe(message.userId(), message.cvId(), "vectorizing", "Resume parsing complete, vectorizing data...", parsingTime);

            long vectorizeStartTime = System.currentTimeMillis();
            vectorizationClient.vectorizeCv(new ResumeRequest(message.userId(), message.cvId(), res));
            long vectorizeEndTime = System.currentTimeMillis();
            double vectorizeTime = Math.round((vectorizeEndTime - vectorizeStartTime) / 100.0) / 10.0;

            // Chỉ gọi markReady() sau khi parsing và vectorization đều thành công
            cv.markReady();
            resumeRepository.save(cv);

            sendSseSafe(message.userId(), message.cvId(), "analyzed", "Resume analysis complete", vectorizeTime);

        } catch (Exception e) {
            log.error("Error processing resume: {}", message.cvId(), e);

            // Nếu parsing hoặc vectorization thực sự thất bại, gọi markAnalysisFailed() rồi rethrow exception
            try {
                cv.markAnalysisFailed();
                resumeRepository.save(cv);
            } catch (Exception dbEx) {
                log.error("Failed to mark resume {} as ANALYSIS_FAILED in database", message.cvId(), dbEx);
            }

            // Exception từ SSE không được làm thay đổi trạng thái nghiệp vụ và không được khiến Rabbit retry
            sendSseSafe(message.userId(), message.cvId(), "failed", "Resume analysis failed", null);

            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException("Resume processing failed for cv: " + message.cvId(), e);
        }
    }

    private void sendSseSafe(long userId, long cvId, String status, String message, Double executionTime) {
        try {
            var builder = SseMessagePayload.builder()
                    .id(cvId)
                    .status(status)
                    .message(message);
            if (executionTime != null) {
                builder.executionTime(executionTime);
            }
            log.info("Sending SSE resume-process event: userId={}, cvId={}, status={}, executionTime={}", userId, cvId, status, executionTime);
            sseEmitterService.sendEvent(userId, "resume-process", builder.build());
        } catch (Exception sseEx) {
            log.warn("Failed to send SSE event ({}) for cv: {}", status, cvId, sseEx);
        }
    }
}
