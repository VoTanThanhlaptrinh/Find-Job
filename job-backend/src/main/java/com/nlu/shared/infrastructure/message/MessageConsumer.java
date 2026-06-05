package com.nlu.shared.infrastructure.message;

import com.nlu.applicationProcess.api.dto.client.ResumeParsingMessage;
import com.nlu.applicationProcess.api.dto.client.ResumeRequest;
import com.nlu.shared.api.message.dto.ApiMessage;
import com.nlu.shared.api.message.dto.CloudUploadMessage;
import com.nlu.applicationProcess.application.ResumeParsingService;
import com.nlu.applicationProcess.application.VectorizationClient;
import com.nlu.applicationProcess.application.ResumeService;
import com.nlu.shared.application.SseNotificationService;
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
    private final ResumeService resumeService;
    private final SseNotificationService sseNotificationService;
	@RabbitListener(queues = "mailQueue")
    public void receiveMail(@Payload MailMessage message) {
        mailService.sendMessage(message.getTo(), message.getSubject(), message.getContent());
    }
    @RabbitListener(queues = "parsingQueue")
    public void parsingRawText(@Payload ResumeParsingMessage message){
        try {
            var res = resumeParsingService.processResume(message.rawText());
            log.info(res.toString());
            vectorizationClient.vectorizeCv(new ResumeRequest(message.userId(), message.cvId(),res));
            sseNotificationService.sendNotification(message.cvId(), "completed");
        } catch (Exception e) {
            log.error("Error processing resume: {}", message.cvId(), e);
            sseNotificationService.sendNotification(message.cvId(), "failed");
        }

    }
    @RabbitListener(queues = "apiQueue")
    public void processApiService(@Payload ApiMessage message) {
        switch (message.getOperationType()) {
            case VECTORIZE_CV -> vectorizationClient.vectorizeCv(message.getResumeRequest());
            case VECTORIZE_JD -> vectorizationClient.vectorizeJd(message.getVectorizeJdRequest());
        }
    }
    @RabbitListener(queues = "cloudUploadQueue")
    public void uploadToCloud(@Payload CloudUploadMessage message) {
        resumeService.uploadResumeToCloud(message.data(), message.key(), message.originalName());
    }
}


