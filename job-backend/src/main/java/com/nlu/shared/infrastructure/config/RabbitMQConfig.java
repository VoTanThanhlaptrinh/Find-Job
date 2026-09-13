package com.nlu.shared.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration with durable topology v2 for Mail, Resume parsing, and Job vectorization.
 *
 * CUTOVER NOTE:
 * Legacy queues ('mailQueue', 'parsingQueue', 'apiQueue') are NOT automatically deleted to prevent
 * accidental data loss during migration. Once all legacy messages have been fully drained/consumed,
 * administrators should manually clean up the legacy queues and exchanges via RabbitMQ Management.
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

	// Topology v2 constants
	public static final String MAIL_QUEUE_V2 = "mailQueue.v2";
	public static final String MAIL_EXCHANGE_V2 = "mailExchange.v2";
	public static final String MAIL_ROUTING_KEY_V2 = "mailRoutingKey.v2";

	public static final String PARSING_QUEUE_V2 = "parsingQueue.v2";
	public static final String PARSING_EXCHANGE_V2 = "parsingExchange.v2";
	public static final String PARSING_ROUTING_KEY_V2 = "parsingRoutingKey.v2";

	public static final String API_QUEUE_V2 = "apiQueue.v2";
	public static final String API_EXCHANGE_V2 = "apiExchange.v2";
	public static final String API_ROUTING_KEY_V2 = "apiRoutingKey.v2";

	// Legacy cloud upload constants (unchanged)
	public static final String CLOUD_UPLOAD_QUEUE = "cloudUploadQueue";
	public static final String CLOUD_UPLOAD_EXCHANGE = "cloudUploadExchange";
	public static final String CLOUD_UPLOAD_ROUTING_KEY = "cloudUploadRoutingKey";

	/* Config for mail message queue (v2 - durable) */
	@Bean
	Queue mailQueue() {
		return new Queue(MAIL_QUEUE_V2, true);
	}

	@Bean
	DirectExchange mailExchange() {
		return new DirectExchange(MAIL_EXCHANGE_V2, true, false);
	}

	@Bean
	Binding mailBinding(Queue mailQueue, DirectExchange mailExchange) {
		return BindingBuilder.bind(mailQueue).to(mailExchange).with(MAIL_ROUTING_KEY_V2);
	}

	/* Config for AI parsing message queue (v2 - durable) */
	@Bean
	Queue parsingQueue() {
		return new Queue(PARSING_QUEUE_V2, true);
	}

	@Bean
	DirectExchange parsingExchange() {
		return new DirectExchange(PARSING_EXCHANGE_V2, true, false);
	}

	@Bean
	Binding parsingBinding(Queue parsingQueue, DirectExchange parsingExchange) {
		return BindingBuilder.bind(parsingQueue).to(parsingExchange).with(PARSING_ROUTING_KEY_V2);
	}

	/* Config for API / Job vectorization message queue (v2 - durable) */
	@Bean
	Queue apiQueue() {
		return new Queue(API_QUEUE_V2, true);
	}

	@Bean
	DirectExchange apiExchange() {
		return new DirectExchange(API_EXCHANGE_V2, true, false);
	}

	@Bean
	Binding apiBinding(Queue apiQueue, DirectExchange apiExchange) {
		return BindingBuilder.bind(apiQueue).to(apiExchange).with(API_ROUTING_KEY_V2);
	}

	/* Config for Cloud upload message queue (unchanged) */
	@Bean
	Queue cloudUploadQueue() {
		return new Queue(CLOUD_UPLOAD_QUEUE, false);
	}

	@Bean
	DirectExchange cloudUploadExchange() {
		return new DirectExchange(CLOUD_UPLOAD_EXCHANGE);
	}

	@Bean
	Binding cloudUploadBinding(Queue cloudUploadQueue, DirectExchange cloudUploadExchange) {
		return BindingBuilder.bind(cloudUploadQueue).to(cloudUploadExchange).with(CLOUD_UPLOAD_ROUTING_KEY);
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(jsonMessageConverter());
		template.addBeforePublishPostProcessors(message -> {
			message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
			return message;
		});
		return template;
	}

	@Bean
	public MessageConverter jsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}
}
