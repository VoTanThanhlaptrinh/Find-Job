package com.job_web.service.application;

import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.application.ResumeDTO;
import com.job_web.dto.application.ResumeDetailDTO;
import com.job_web.dto.application.ResumeUploadDTO;
import com.job_web.models.Resume;

import java.security.Principal;
import java.util.List;

public interface ResumeService {
    public ApiResponse<List<ResumeDTO>> getListResumeOfUser(Principal principal);

    public Resume findById(long id);

    public ApiResponse<ResumeDetailDTO> getResumeDetail(long id, Principal principal);

    public ApiResponse<String> createResume(ResumeUploadDTO resumeUploadDTO, Principal principal);

    public ApiResponse<String> updateResume(long id, ResumeUploadDTO resumeUploadDTO, Principal principal);

    public ApiResponse<String> deleteResume(long id, Principal principal);
}



