package com.job_web.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {
	@Bean
	Queue queue() {
		return new Queue("mailQueue", false);
	}

	@Bean
	DirectExchange exchange() {
		return new DirectExchange("mailExchange");
	}

	@Bean
	Binding binding(Queue queue, DirectExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).with("mailRoutingKey");
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
	    RabbitTemplate template = new RabbitTemplate(connectionFactory);
	    template.setMessageConverter(jsonMessageConverter());
	    return template;
	}

	@Bean
	public Jackson2JsonMessageConverter jsonMessageConverter() {
	    return new Jackson2JsonMessageConverter();
	}
}
