package com.job_web.service.application;

import com.job_web.dto.application.ResumeUploadDTO;
import com.job_web.dto.application.ResumeUrlDTO;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.application.ResumeDTO;
import com.job_web.dto.application.ResumeDetailDTO;
import com.job_web.dto.application.ResumeView;
import com.job_web.models.User;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface ResumeService {
    ApiResponse<List<ResumeView>> getListResumeOfUser(User user);

    ApiResponse<List<ResumeDTO>> getResumesByUser(String email);

    ApiResponse<ResumeDetailDTO> getResumeDetail(long id, User user);

    ApiResponse<ResumeView> createResume(ResumeUploadDTO resumeUploadDTO, User user);

    ApiResponse<String> updateResume(long id, ResumeUploadDTO resumeUploadDTO, User user);

    ApiResponse<String> deleteResume(long id, User user);

    void uploadResumeToCloud(byte[] data, String key, String originalName);
    byte[] toByteArray(InputStream inputStream) throws IOException;

    ApiResponse<ResumeUrlDTO> getResumeViewUrl(long id, User user);

    ApiResponse<ResumeUrlDTO> getResumeDownloadUrl(long id, User user);

    ApiResponse<ResumeUrlDTO> getResumeViewUrlForHirer(long id);

    ApiResponse<ResumeUrlDTO> getResumeDownloadUrlForHirer(long id);
}
