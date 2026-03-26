package com.job_web.service.ai;


import com.job_web.dto.ai.ResumeRequest;

public interface ApiService {
    void callFastApi(ResumeRequest resumeEmbeddingRequest);
}
