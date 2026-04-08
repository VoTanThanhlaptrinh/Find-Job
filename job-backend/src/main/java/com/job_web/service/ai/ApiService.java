package com.job_web.service.ai;


import com.job_web.dto.ai.ResumeRequest;
import com.job_web.dto.job.VectorizeJdRequest;

public interface ApiService {
    void vectorizeCv(ResumeRequest request);

    void vectorizeJd(VectorizeJdRequest request);
}
