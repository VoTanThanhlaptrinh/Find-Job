package com.job_web.message;

import com.job_web.dto.ai.ResumeParsingMessage;
import com.job_web.dto.ai.ResumeRequest;
import com.job_web.dto.message.ApiMessage;
import com.job_web.dto.message.CloudUploadMessage;
import com.job_web.service.ai.AIService;
import com.job_web.service.ai.ApiService;
import com.job_web.service.application.ResumeService;
import com.job_web.service.notification.SseNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.job_web.dto.message.MailMessage;
import com.job_web.service.notification.MailService;

import lombok.AllArgsConstructor;
@Slf4j
@AllArgsConstructor
@Component
public class MessageConsumer {

	private final MailService mailService;
    private final AIService aiService;
    private final ApiService apiService;
    private final ResumeService resumeService;
    private final SseNotificationService sseNotificationService;

	@RabbitListener(queues = "mailQueue")
    public void receiveMail(@Payload MailMessage message) {
        mailService.sendMessage(message.getTo(), message.getSubject(), message.getContent());
    }
    @RabbitListener(queues = "parsingQueue")
    public void parsingRawText(@Payload ResumeParsingMessage message){
        try {
            var res = aiService.processResume(message.rawText());
            log.info(res.toString());
            // Gửi SSE notification khi AI xử lý xong
            apiService.vectorizeCv(new ResumeRequest(message.userId(), message.cvId(),res));
            sseNotificationService.sendNotification(message.cvId(), "completed");
        } catch (Exception e) {
            log.error("Error processing resume: {}", message.cvId(), e);
            sseNotificationService.sendNotification(message.cvId(), "failed");
        }

    }
    @RabbitListener(queues = "apiQueue")
    public void processApiService(@Payload ApiMessage message) {
        switch (message.getOperationType()) {
            case VECTORIZE_CV -> apiService.vectorizeCv(message.getResumeRequest());
            case VECTORIZE_JD -> apiService.vectorizeJd(message.getVectorizeJdRequest());
        }
    }
    @RabbitListener(queues = "cloudUploadQueue")
    public void uploadToCloud(@Payload CloudUploadMessage message) {
        resumeService.uploadResumeToCloud(message.data(), message.key(), message.originalName());
    }
}


