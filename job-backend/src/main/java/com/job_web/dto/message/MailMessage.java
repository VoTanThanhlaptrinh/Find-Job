package com.job_web.dto.message;

public record MailMessage(
        String to,
        String subject,
        String content
) {
    public String getTo() {
        return to;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }
}
