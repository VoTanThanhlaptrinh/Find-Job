package com.job_web.message;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.job_web.dto.MailMessage;
import com.job_web.service.IMailService;

import lombok.AllArgsConstructor;
@AllArgsConstructor
@Component
public class MailConsumer {

	private final IMailService mailService;
	@RabbitListener(queues = "mailQueue")
    public void receiveMail(@Payload MailMessage message) {
        mailService.sendMessage(message.getTo(), message.getSubject(), message.getContent());
    }
}
