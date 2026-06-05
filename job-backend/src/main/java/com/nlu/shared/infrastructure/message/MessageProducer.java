package com.nlu.shared.infrastructure.message;

import com.nlu.applicationProcess.api.dto.client.ResumeParsingMessage;
import com.nlu.shared.api.message.dto.CloudUploadMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.nlu.shared.api.message.dto.MailMessage;

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


