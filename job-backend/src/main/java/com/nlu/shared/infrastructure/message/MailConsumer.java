package com.nlu.shared.infrastructure.message;

import com.nlu.shared.api.message.dto.MailMessage;
import com.nlu.shared.application.MailService;
import com.nlu.shared.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailConsumer {

    private final MailService mailService;

    @RabbitListener(queues = RabbitMQConfig.MAIL_QUEUE_V2)
    public void receiveMail(@Payload MailMessage message) {
        log.info("Received mail message for recipient: {}", message.getTo());
        mailService.sendMessage(message.getTo(), message.getSubject(), message.getContent());
    }
}
