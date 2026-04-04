package com.job_web.service.application;

import com.job_web.dto.application.ResumeUploadDTO;
import com.job_web.dto.application.ResumeUrlDTO;
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

    ApiResponse<ResumeView> createResume(ResumeUploadDTO resumeUploadDTO, Principal principal);

    ApiResponse<String> updateResume(long id, ResumeUploadDTO resumeUploadDTO, Principal principal);

    ApiResponse<String> deleteResume(long id, Principal principal);

    void uploadResumeToCloud(byte[] data, String key, String originalName);
    byte[] toByteArray(InputStream inputStream) throws IOException;

    /**
     * Lấy Pre-signed URL để xem resume trực tiếp (inline) trên trình duyệt.
     *
     * @param id        ID của resume
     * @param principal Thông tin người dùng đang đăng nhập
     * @return ApiResponse chứa ResumeUrlDTO với URL xem resume
     */
    ApiResponse<ResumeUrlDTO> getResumeViewUrl(long id, Principal principal);

    /**
     * Lấy Pre-signed URL để tải resume về (attachment).
     *
     * @param id        ID của resume
     * @param principal Thông tin người dùng đang đăng nhập
     * @return ApiResponse chứa ResumeUrlDTO với URL tải resume
     */
    ApiResponse<ResumeUrlDTO> getResumeDownloadUrl(long id, Principal principal);

    /**
     * Lấy Pre-signed URL để xem resume (dành cho Hirer).
     *
     * @param id ID của resume
     * @return ApiResponse chứa ResumeUrlDTO với URL xem resume
     */
    ApiResponse<ResumeUrlDTO> getResumeViewUrlForHirer(long id);

    /**
     * Lấy Pre-signed URL để tải resume (dành cho Hirer).
     *
     * @param id ID của resume
     * @return ApiResponse chứa ResumeUrlDTO với URL tải resume
     */
    ApiResponse<ResumeUrlDTO> getResumeDownloadUrlForHirer(long id);
}

