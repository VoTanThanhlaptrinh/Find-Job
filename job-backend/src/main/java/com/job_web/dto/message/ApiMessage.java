package com.job_web.dto.message;

import com.job_web.dto.ai.ResumeRequest;
import com.job_web.dto.job.VectorizeJdRequest;

public record ApiMessage(
    ApiOperationType operationType,
    ResumeRequest resumeRequest,
    VectorizeJdRequest vectorizeJdRequest
) {
    public enum ApiOperationType {
        VECTORIZE_CV,
        VECTORIZE_JD
    }

    public static ApiMessage vectorizeCv(ResumeRequest request) {
        return new ApiMessage(ApiOperationType.VECTORIZE_CV, request, null);
    }

    public static ApiMessage vectorizeJd(VectorizeJdRequest request) {
        return new ApiMessage(ApiOperationType.VECTORIZE_JD, null, request);
    }

    public ApiOperationType getOperationType() {
        return operationType;
    }

    public ResumeRequest getResumeRequest() {
        return resumeRequest;
    }

    public VectorizeJdRequest getVectorizeJdRequest() {
        return vectorizeJdRequest;
    }
}
