package com.job_web.service.application;

import com.job_web.dto.application.ResumeUploadDTO;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.application.ResumeDTO;
import com.job_web.dto.application.ResumeDetailDTO;
import com.job_web.dto.application.ResumeView;
import com.job_web.models.Resume;

import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.List;

public interface ResumeService {
    ApiResponse<List<ResumeView>> getListResumeOfUser(Principal principal);

    ApiResponse<List<ResumeDTO>> getResumesByUser(String email);

    ApiResponse<ResumeDetailDTO> getResumeDetail(long id, Principal principal);

    ApiResponse<String> createResume(ResumeUploadDTO resumeUploadDTO, Principal principal);

    ApiResponse<String> updateResume(long id, ResumeUploadDTO resumeUploadDTO, Principal principal);

    ApiResponse<String> deleteResume(long id, Principal principal);

    void uploadResumeToCloud(byte[] data, String key);
    byte[] toByteArray(InputStream inputStream) throws IOException;
}

