package com.job_web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean
    public RestClient fastApiClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 5 giây để kết nối
        factory.setReadTimeout(60000);    // 60 giây để đợi FastAPI xử lý xong (tăng lên nếu model AI nặng)

        return builder
                .baseUrl("http://localhost:8000")
                .requestFactory(factory)
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
