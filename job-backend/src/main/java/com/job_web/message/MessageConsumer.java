package com.job_web.message;

import com.job_web.dto.ai.ResumeModel;
import com.job_web.service.ai.AIService;
import com.job_web.service.ai.ApiService;
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
	@RabbitListener(queues = "mailQueue")
    public void receiveMail(@Payload MailMessage message) {
        mailService.sendMessage(message.getTo(), message.getSubject(), message.getContent());
    }
    @RabbitListener(queues = "parsingQueue")
    public void parsingRawText(@Payload String rawText,@Payload long userId,@Payload long cvId){
        aiService.processResume(rawText);
    }
}


