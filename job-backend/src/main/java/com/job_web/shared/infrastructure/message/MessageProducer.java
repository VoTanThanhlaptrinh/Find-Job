package com.job_web.shared.infrastructure.message;

import com.job_web.application_process.infrastructure.ai.dto.ResumeParsingMessage;
import com.job_web.shared.api.message.dto.CloudUploadMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.job_web.shared.api.message.dto.MailMessage;

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


