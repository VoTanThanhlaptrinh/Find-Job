package com.nlu.shared.application.impl;

import java.time.Duration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.nlu.shared.application.MailService;

@Slf4j
@Service
public class MailServiceImpl implements MailService {

    private final RestClient restClient;
    private final String endpoint;
    private final String apiKey;

    @Autowired
    public MailServiceImpl(
            RestClient.Builder restClientBuilder,
            @Value("${app.mail-service.base-url:https://mail-service-tau.vercel.app/}") String baseUrl,
            @Value("${app.mail-service.api-key:}") String apiKey,
            @Value("${app.mail-service.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${app.mail-service.read-timeout-ms:10000}") int readTimeoutMs) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.endpoint = resolveEndpoint(baseUrl);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
    }

    public MailServiceImpl(RestClient restClient, String baseUrl, String apiKey) {
        this.restClient = restClient;
        this.endpoint = resolveEndpoint(baseUrl);
        this.apiKey = apiKey != null ? apiKey.trim() : "";
    }

    @Override
    public void sendMessage(String to, String subject, String text) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Recipient email ('to') must not be blank");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Email subject must not be blank");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Email body ('text') must not be blank");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("MAIL_SERVICE_API_KEY is not configured");
        }

        // PII: 'to' is an email address — never log it.
        log.info("Sending email via Mail Service — subject: {}", subject);

        try {
            restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(new MailSendRequest(to, subject, text))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Email sent successfully via Mail Service — subject: {}", subject);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Failed to send email via Mail Service: HTTP " + e.getStatusCode() + " - " + e.getResponseBodyAsString(),
                    e);
        } catch (RestClientException e) {
            throw new IllegalStateException("Failed to send email via Mail Service: " + e.getMessage(), e);
        }
    }

    private static String resolveEndpoint(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://mail-service-tau.vercel.app/api/mails/send";
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.endsWith("/api")) {
            return trimmed + "/mails/send";
        }
        return trimmed + "/api/mails/send";
    }

    String getEndpoint() {
        return endpoint;
    }

    String getApiKey() {
        return apiKey;
    }

    private record MailSendRequest(String to, String subject, String body) {
    }
}
