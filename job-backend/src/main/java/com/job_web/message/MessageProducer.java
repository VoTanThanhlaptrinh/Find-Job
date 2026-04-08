package com.job_web.message;

import com.job_web.dto.ai.ResumeParsingMessage;
import com.job_web.dto.message.ApiMessage;
import com.job_web.dto.message.CloudUploadMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.job_web.dto.message.MailMessage;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Component
public class MessageProducer {
	private final RabbitTemplate rabbitTemplate;

	public void sendMail(MailMessage message) {
		rabbitTemplate.convertAndSend("mailExchange", "mailRoutingKey", message);
	}
	public void processAI(ResumeParsingMessage message) {
		rabbitTemplate.convertAndSend("parsingExchange", "parsingRoutingKey", message);
	}

	public void uploadToCloud(CloudUploadMessage message) {
		rabbitTemplate.convertAndSend("cloudUploadExchange", "cloudUploadRoutingKey", message);
	}
}


