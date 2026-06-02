package com.job_web.application_process.application;


import com.job_web.application_process.infrastructure.ai.dto.ResumeRequest;
import com.job_web.recruiment.api.dto.VectorizeJdRequest;

public interface ApiService {
    void vectorizeCv(ResumeRequest request);

    void vectorizeJd(VectorizeJdRequest request);
}
