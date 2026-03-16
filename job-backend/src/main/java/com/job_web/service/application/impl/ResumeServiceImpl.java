package com.job_web.service.application.impl;

import com.job_web.data.ResumeRepository;
import com.job_web.data.UserRepository;
import com.job_web.dto.application.ResumeDTO;
import com.job_web.dto.application.ResumeDetailDTO;
import com.job_web.dto.application.ResumeUploadDTO;
import com.job_web.dto.common.ApiResponse;
import com.job_web.models.Resume;
import com.job_web.models.User;
import com.job_web.service.application.ResumeService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
@Service
@AllArgsConstructor
public class ResumeServiceImpl implements ResumeService {
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    @Override
    public ApiResponse<List<ResumeDTO>> getListResumeOfUser(Principal principal) {
        List<ResumeDTO> res = resumeRepository.findAllByUser(principal.getName());
        String message = res.isEmpty() ? "The user has not uploaded any resumes yet." : "success";
        return new ApiResponse<>(message,res, HttpStatus.OK.value());
    }

    @Override
    public Resume findById(long id) {
        return resumeRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
    }

    @Override
    public ApiResponse<ResumeDetailDTO> getResumeDetail(long id, Principal principal) {
        if (principal == null) {
            return new ApiResponse<>("You are not logged in.", null, HttpStatus.UNAUTHORIZED.value());
        }
        Optional<Resume> cvOpt = resumeRepository.findById(id);
        if (cvOpt.isEmpty()) {
            return new ApiResponse<>("Resume not found.", null, HttpStatus.NOT_FOUND.value());
        }
        Resume cv = cvOpt.get();
        if (cv.getUser() == null || !principal.getName().equals(cv.getUser().getEmail())) {
            return new ApiResponse<>("You do not have permission to access this resume.", null, HttpStatus.FORBIDDEN.value());
        }
        ResumeDetailDTO detail = new ResumeDetailDTO(cv.getId(), cv.getFileName(), cv.getCreateDate());
        return new ApiResponse<>("success", detail, HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<String> createResume(ResumeUploadDTO resumeUploadDTO, Principal principal) {
        if (principal == null) {
            return new ApiResponse<>("You are not logged in.", null, HttpStatus.UNAUTHORIZED.value());
        }
        Optional<User> userOpt = userRepository.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return new ApiResponse<>("User not found.", null, HttpStatus.NOT_FOUND.value());
        }
        Resume cv = new Resume();
        cv.setUser(userOpt.get());
        cv.setFileName(resumeUploadDTO.getFile().getOriginalFilename());
        try {
            cv.setData(resumeUploadDTO.getFile().getBytes());
        } catch (IOException e) {
            return new ApiResponse<>("Unable to read the resume file.", null, HttpStatus.BAD_REQUEST.value());
        }
        resumeRepository.save(cv);
        return new ApiResponse<>("success", null, HttpStatus.CREATED.value());
    }

    @Override
    public ApiResponse<String> updateResume(long id, ResumeUploadDTO resumeUploadDTO, Principal principal) {
        if (principal == null) {
            return new ApiResponse<>("You are not logged in.", null, HttpStatus.UNAUTHORIZED.value());
        }
        Optional<Resume> cvOpt = resumeRepository.findById(id);
        if (cvOpt.isEmpty()) {
            return new ApiResponse<>("Resume not found.", null, HttpStatus.NOT_FOUND.value());
        }
        Resume cv = cvOpt.get();
        if (cv.getUser() == null || !principal.getName().equals(cv.getUser().getEmail())) {
            return new ApiResponse<>("You do not have permission to edit this resume.", null, HttpStatus.FORBIDDEN.value());
        }
        cv.setFileName(resumeUploadDTO.getFile().getOriginalFilename());
        try {
            cv.setData(resumeUploadDTO.getFile().getBytes());
        } catch (IOException e) {
            return new ApiResponse<>("Unable to read the resume file.", null, HttpStatus.BAD_REQUEST.value());
        }
        resumeRepository.save(cv);
        return new ApiResponse<>("success", null, HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<String> deleteResume(long id, Principal principal) {
        if (principal == null) {
            return new ApiResponse<>("You are not logged in.", null, HttpStatus.UNAUTHORIZED.value());
        }
        Optional<Resume> cvOpt = resumeRepository.findById(id);
        if (cvOpt.isEmpty()) {
            return new ApiResponse<>("Resume not found.", null, HttpStatus.NOT_FOUND.value());
        }
        Resume cv = cvOpt.get();
        if (cv.getUser() == null || !principal.getName().equals(cv.getUser().getEmail())) {
            return new ApiResponse<>("You do not have permission to delete this resume.", null, HttpStatus.FORBIDDEN.value());
        }
        resumeRepository.delete(cv);
        return new ApiResponse<>("success", null, HttpStatus.OK.value());
    }
}