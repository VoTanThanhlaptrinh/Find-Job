package com.nlu.shared.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(FileServiceProperties.class)
public class FileServiceClientConfig {

    @Bean
    RestClient fileServiceRestClient(RestClient.Builder builder, FileServiceProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));

        RestClient.Builder configuredBuilder = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("Accept", "application/json");
        if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
            configuredBuilder.defaultHeader("x-api-key", properties.apiKey());
        }
        return configuredBuilder.build();
    }

    @Bean
    RestClient fileDownloadRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        return builder.requestFactory(requestFactory).build();
    }
}
