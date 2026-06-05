package com.nlu.applicationProcess.application;


import com.nlu.applicationProcess.api.dto.client.ResumeRequest;
import com.nlu.recruitment.api.dto.VectorizeJdRequest;

public interface VectorizationClient {
    void vectorizeCv(ResumeRequest request);

    void vectorizeJd(VectorizeJdRequest request);
}
