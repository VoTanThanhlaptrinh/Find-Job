package com.job_web.service.notification.impl;

import java.util.Properties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.job_web.service.notification.MailService;

@Slf4j
@Configuration
public class MailServiceImp implements MailService {
	@Value("${spring.mail.username}")
	private  String from;
	@Value("${spring.mail.password}")
	private  String password;


    @Override
	public void sendMessage(String to, String subject, String text) {
		// PII: 'to' is an email address — never log it.
		// MDC userId/traceId is set by the caller (message consumer).
		log.info("Sending email — subject: {}", subject);

		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(to);
		message.setSubject(subject);
		message.setText(text);
		getJavaMailSender().send(message);

		log.info("Email sent successfully — subject: {}", subject);
	}

	@Bean
	JavaMailSender getJavaMailSender() {
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost("smtp.gmail.com");
		mailSender.setPort(587);

		mailSender.setUsername(this.from);
		mailSender.setPassword(this.password);

		Properties props = mailSender.getJavaMailProperties();
		props.put("mail.transport.protocol", "smtp");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");

		return mailSender;
	}
}
