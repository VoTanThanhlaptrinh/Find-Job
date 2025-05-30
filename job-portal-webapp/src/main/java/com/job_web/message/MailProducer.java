package com.job_web.message;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.job_web.dto.MailMessage;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Component
public class MailProducer {
	private final RabbitTemplate rabbitTemplate;

	public void sendMail(MailMessage message) {
		rabbitTemplate.convertAndSend("mailExchange", "mailRoutingKey", message);
	}
}
