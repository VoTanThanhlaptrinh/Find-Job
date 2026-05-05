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
    List<ResumeView> getListResumeOfUser(User user);

    List<ResumeDTO> getResumesByUser(String email);

    ResumeDetailDTO getResumeDetail(long id, User user);

    ResumeView createResume(ResumeUploadDTO resumeUploadDTO, User user);

    void updateResume(long id, ResumeUploadDTO resumeUploadDTO, User user);

    void deleteResume(long id, User user);

    void uploadResumeToCloud(byte[] data, String key, String originalName);
    byte[] toByteArray(InputStream inputStream) throws IOException;

    ResumeUrlDTO getResumeViewUrl(long id, User user);

    ResumeUrlDTO getResumeDownloadUrl(long id, User user);

    ResumeUrlDTO getResumeViewUrlForHirer(long id);

    ResumeUrlDTO getResumeDownloadUrlForHirer(long id);
}
