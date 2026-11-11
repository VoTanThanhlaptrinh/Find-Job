package com.nlu.shared.infrastructure.message;

import com.nlu.applicationProcess.api.dto.client.ResumeParsingMessage;
import com.nlu.recruitment.api.dto.VectorizeJdRequest;
import com.nlu.shared.api.message.dto.ApiMessage;
import com.nlu.shared.api.message.dto.MailMessage;
import com.nlu.shared.infrastructure.config.RabbitMQConfig;
import lombok.AllArgsConstructor;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class MessageProducer {
	private final RabbitTemplate rabbitTemplate;

	public void sendMail(MailMessage message) {
		rabbitTemplate.convertAndSend(
			RabbitMQConfig.MAIL_EXCHANGE_V2,
			RabbitMQConfig.MAIL_ROUTING_KEY_V2,
			message,
			m -> {
				m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
				return m;
			}
		);
	}

	public void processAI(ResumeParsingMessage message) {
		rabbitTemplate.convertAndSend(
			RabbitMQConfig.PARSING_EXCHANGE_V2,
			RabbitMQConfig.PARSING_ROUTING_KEY_V2,
			message,
			m -> {
				m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
				return m;
			}
		);
	}

	public void processJdVectorize(VectorizeJdRequest request) {
		rabbitTemplate.convertAndSend(
			RabbitMQConfig.API_EXCHANGE_V2,
			RabbitMQConfig.API_ROUTING_KEY_V2,
			ApiMessage.vectorizeJd(request),
			m -> {
				m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
				return m;
			}
		);
	}
}
