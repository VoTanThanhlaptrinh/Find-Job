package com.job_web.service.ai.impl;

import com.job_web.dto.ai.ResumeRequest;
import com.job_web.service.ai.ApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Service
@RequiredArgsConstructor
public class ApiServiceImpl implements ApiService {
    private final RestClient restClient;

    @Override
    public void callFastApi(ResumeRequest request) {

    }
}
