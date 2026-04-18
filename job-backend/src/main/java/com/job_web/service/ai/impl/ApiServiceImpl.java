package com.job_web.service.ai.impl;

import com.job_web.dto.ai.ResumeRequest;
import com.job_web.dto.job.VectorizeJdRequest;
import com.job_web.service.ai.ApiService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class ApiServiceImpl implements ApiService {
    private final RestClient restClient;

    private static final String MDC_USER_ID = "userId";
    private static final String MDC_CV_ID = "cvId";
    private static final String MDC_JOB_ID = "jobId";

    public ApiServiceImpl(RestClient.Builder restClientBuilder, @Value("${spring.ai.agent.base-url}") String baseUrl) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public void vectorizeCv(ResumeRequest request) {
        try {
            MDC.put(MDC_USER_ID, String.valueOf(request.userId()));
            MDC.put(MDC_CV_ID, String.valueOf(request.cvId()));

            log.info("Dispatching CV vectorization to AI agent for user: {}, cv: {}",
                    request.userId(), request.cvId());

            long startTime = System.currentTimeMillis();

            restClient.post()
                    .uri("/vectorize-resume/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            long duration = System.currentTimeMillis() - startTime;
            log.info("CV vectorization completed in {}ms for user: {}, cv: {}",
                    duration, request.userId(), request.cvId());
        } finally {
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_CV_ID);
        }
    }

    @Override
    public void vectorizeJd(VectorizeJdRequest request) {
        try {
            MDC.put(MDC_JOB_ID, String.valueOf(request.getJobId()));

            log.info("Dispatching JD vectorization to AI agent for job: {}", request.getJobId());

            long startTime = System.currentTimeMillis();

            restClient.post()
                    .uri("/vectorize-jd/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            long duration = System.currentTimeMillis() - startTime;
            log.info("JD vectorization completed in {}ms for job: {}", duration, request.getJobId());
        } finally {
            MDC.remove(MDC_JOB_ID);
        }
    }
}
