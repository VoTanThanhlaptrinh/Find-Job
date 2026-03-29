package com.job_web.message;

import com.job_web.dto.ai.ResumeParsingMessage;
import com.job_web.dto.message.ApiMessage;
import com.job_web.dto.message.CloudUploadMessage;
import com.job_web.service.ai.AIService;
import com.job_web.service.ai.ApiService;
import com.job_web.service.application.ResumeService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.job_web.dto.message.MailMessage;
import com.job_web.service.notification.MailService;

import lombok.AllArgsConstructor;
@AllArgsConstructor
@Component
public class MessageConsumer {

	private final MailService mailService;
    private final AIService aiService;
    private final ApiService apiService;
    private final ResumeService resumeService;

	@RabbitListener(queues = "mailQueue")
    public void receiveMail(@Payload MailMessage message) {
        mailService.sendMessage(message.getTo(), message.getSubject(), message.getContent());
    }
    @RabbitListener(queues = "parsingQueue")
    public void parsingRawText(@Payload ResumeParsingMessage message){
        aiService.processResume(message.rawText());
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


