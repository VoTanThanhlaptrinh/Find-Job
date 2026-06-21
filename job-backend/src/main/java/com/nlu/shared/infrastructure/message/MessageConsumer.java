package com.nlu.shared.infrastructure.message;

import com.nlu.applicationProcess.api.dto.client.ResumeParsingMessage;
import com.nlu.applicationProcess.api.dto.client.ResumeRequest;
import com.nlu.shared.api.message.dto.ApiMessage;
import com.nlu.applicationProcess.application.ResumeParsingService;
import com.nlu.applicationProcess.application.VectorizationClient;
import com.nlu.shared.application.SseEmitterService;
import com.nlu.shared.domain.model.SseMessagePayload;
import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.recruitment.domain.repository.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.nlu.shared.api.message.dto.MailMessage;
import com.nlu.shared.application.MailService;

import lombok.AllArgsConstructor;

@Slf4j
@AllArgsConstructor
@Component
public class MessageConsumer {

    private final MailService mailService;
    private final ResumeParsingService resumeParsingService;
    private final VectorizationClient vectorizationClient;
    private final SseEmitterService sseEmitterService;
    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;

    @RabbitListener(queues = "mailQueue")
    public void receiveMail(@Payload MailMessage message) {
        mailService.sendMessage(message.getTo(), message.getSubject(), message.getContent());
    }

    @RabbitListener(queues = "parsingQueue")
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
            sseEmitterService.sendEvent(message.userId(), "resume-process", SseMessagePayload.builder()
                    .id(message.cvId())
                    .status("failed")
                    .message("Resume analysis failed")
                    .build());
        }
    }

    @RabbitListener(queues = "apiQueue")
    public void processApiService(@Payload ApiMessage message) {
        try {
            vectorizationClient.vectorizeJd(message.getVectorizeJdRequest());

            // Cập nhật trạng thái isAnalyzed trên Job
            Long jobId = message.getVectorizeJdRequest().getJobId();
            jobRepository.findById(jobId).ifPresent(job -> {
                job.markAnalyzed();
                jobRepository.save(job);
            });

            // SSE event thông báo hoàn tất
            Long userId = message.getVectorizeJdRequest().getUserId();
            sseEmitterService.sendEvent(userId, "job-process",
                SseMessagePayload.builder()
                    .id(jobId)
                    .status("analyzed")
                    .message("Job analysis complete")
                    .build());

        } catch (Exception e) {
            log.error("Failed to vectorize JD for job: {}", message.getVectorizeJdRequest().getJobId(), e);
            // SSE event thông báo thất bại
            Long userId = message.getVectorizeJdRequest().getUserId();
            Long jobId = message.getVectorizeJdRequest().getJobId();
            sseEmitterService.sendEvent(userId, "job-process",
                SseMessagePayload.builder()
                    .id(jobId)
                    .status("failed")
                    .message("Job analysis failed")
                    .build());
        }
    }
}
