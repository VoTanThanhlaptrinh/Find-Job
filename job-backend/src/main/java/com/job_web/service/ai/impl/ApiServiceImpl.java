package com.job_web.service.ai.impl;

import com.job_web.dto.ai.ResumeRequest;
import com.job_web.dto.job.VectorizeJdRequest;
import com.job_web.service.ai.ApiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ApiServiceImpl implements ApiService {
    private final RestClient restClient;

    public ApiServiceImpl(RestClient.Builder restClientBuilder, @Value("${spring.ai.agent.base-url}") String baseUrl) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public void vectorizeCv(ResumeRequest request) {
        restClient.post()
                .uri("/vectorize-resume/")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void vectorizeJd(VectorizeJdRequest request) {
        restClient.post()
                .uri("/vectorize-jd/")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
