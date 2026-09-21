package com.nlu.applicationProcess.application;

import com.nlu.applicationProcess.api.dto.req.ResumeUploadInitiateRequest;
import com.nlu.applicationProcess.api.dto.req.ResumeView;
import com.nlu.applicationProcess.api.dto.res.ResumeUploadInitiateResponse;
import com.nlu.identity.domain.model.User;

import java.util.UUID;

public interface ResumeUploadService {
    ResumeUploadInitiateResponse initiateUpload(String idempotencyKey, ResumeUploadInitiateRequest request, User currentUser);
    ResumeView completeUpload(UUID uploadId, User currentUser);
}
