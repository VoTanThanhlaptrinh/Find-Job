package com.job_web.service;

public interface IMailService {
	public void sendMessage(String to, String subject, String text);
}
