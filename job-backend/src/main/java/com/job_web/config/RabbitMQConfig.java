package com.job_web.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {
	/* Config for mail message queue */
	@Bean
	Queue mailQueue() {
		return new Queue("mailQueue", false);
	}

	@Bean
	DirectExchange mailExchange() {
		return new DirectExchange("mailExchange");
	}

	@Bean
	Binding mailBinding(Queue mailQueue, DirectExchange mailExchange) {
		return BindingBuilder.bind(mailQueue).to(mailExchange).with("mailRoutingKey");
	}
	/* Config for AI service message queue */
	@Bean
	Queue parsingQueue() {
		return new Queue("parsingQueue", false);
	}

	@Bean
	DirectExchange parsingExchange() {
		return new DirectExchange("parsingExchange");
	}

	@Bean
	Binding parsingBinding(Queue parsingQueue, DirectExchange parsingExchange) {
		return BindingBuilder.bind(parsingQueue).to(parsingExchange).with("parsingRoutingKey");
	}
	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
	    RabbitTemplate template = new RabbitTemplate(connectionFactory);
	    template.setMessageConverter(jsonMessageConverter());
	    return template;
	}
	@Bean
	public MessageConverter jsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}
}


