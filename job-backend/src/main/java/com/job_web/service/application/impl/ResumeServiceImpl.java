package com.job_web.service.application.impl;

import com.job_web.data.ResumeRepository;
import com.job_web.data.UserRepository;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.application.ResumeDTO;
import com.job_web.dto.application.ResumeDetailDTO;
import com.job_web.dto.application.ResumeUploadDTO;
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
        String message = res.isEmpty() ? "User chÆ°a upload Resume nÃ o lÃªn háº¿t" : "success";
        return new ApiResponse<>(message,res, HttpStatus.OK.value());
    }

    @Override
    public Resume findById(long id) {
        return resumeRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
    }

    @Override
    public ApiResponse<ResumeDetailDTO> getResumeDetail(long id, Principal principal) {
        if (principal == null) {
            return new ApiResponse<>("Báº¡n chÆ°a Ä‘Äƒng nháº­p", null, HttpStatus.UNAUTHORIZED.value());
        }
        Optional<Resume> cvOpt = resumeRepository.findById(id);
        if (cvOpt.isEmpty()) {
            return new ApiResponse<>("KhÃ´ng tÃ¬m tháº¥y Resume", null, HttpStatus.NOT_FOUND.value());
        }
        Resume cv = cvOpt.get();
        if (cv.getUser() == null || !principal.getName().equals(cv.getUser().getEmail())) {
            return new ApiResponse<>("KhÃ´ng cÃ³ quyá»n truy cáº­p", null, HttpStatus.FORBIDDEN.value());
        }
        ResumeDetailDTO detail = new ResumeDetailDTO(cv.getId(), cv.getFileName(), cv.getCreateDate());
        return new ApiResponse<>("success", detail, HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<String> createResume(ResumeUploadDTO resumeUploadDTO, Principal principal) {
        if (principal == null) {
            return new ApiResponse<>("Báº¡n chÆ°a Ä‘Äƒng nháº­p", null, HttpStatus.UNAUTHORIZED.value());
        }
        Optional<User> userOpt = userRepository.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return new ApiResponse<>("KhÃ´ng tÃ¬m tháº¥y ngÆ°á»i dÃ¹ng", null, HttpStatus.NOT_FOUND.value());
        }
        Resume cv = new Resume();
        cv.setUser(userOpt.get());
        cv.setFileName(resumeUploadDTO.getFile().getOriginalFilename());
        try {
            cv.setData(resumeUploadDTO.getFile().getBytes());
        } catch (IOException e) {
            return new ApiResponse<>("KhÃ´ng thá»ƒ Ä‘á»c file Resume", null, HttpStatus.BAD_REQUEST.value());
        }
        resumeRepository.save(cv);
        return new ApiResponse<>("ThÃ nh cÃ´ng", null, HttpStatus.CREATED.value());
    }

    @Override
    public ApiResponse<String> updateResume(long id, ResumeUploadDTO resumeUploadDTO, Principal principal) {
        if (principal == null) {
            return new ApiResponse<>("Báº¡n chÆ°a Ä‘Äƒng nháº­p", null, HttpStatus.UNAUTHORIZED.value());
        }
        Optional<Resume> cvOpt = resumeRepository.findById(id);
        if (cvOpt.isEmpty()) {
            return new ApiResponse<>("KhÃ´ng tÃ¬m tháº¥y Resume", null, HttpStatus.NOT_FOUND.value());
        }
        Resume cv = cvOpt.get();
        if (cv.getUser() == null || !principal.getName().equals(cv.getUser().getEmail())) {
            return new ApiResponse<>("KhÃ´ng cÃ³ quyá»n chá»‰nh sá»­a", null, HttpStatus.FORBIDDEN.value());
        }
        cv.setFileName(resumeUploadDTO.getFile().getOriginalFilename());
        try {
            cv.setData(resumeUploadDTO.getFile().getBytes());
        } catch (IOException e) {
            return new ApiResponse<>("KhÃ´ng thá»ƒ Ä‘á»c file Resume", null, HttpStatus.BAD_REQUEST.value());
        }
        resumeRepository.save(cv);
        return new ApiResponse<>("ThÃ nh cÃ´ng", null, HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<String> deleteResume(long id, Principal principal) {
        if (principal == null) {
            return new ApiResponse<>("Báº¡n chÆ°a Ä‘Äƒng nháº­p", null, HttpStatus.UNAUTHORIZED.value());
        }
        Optional<Resume> cvOpt = resumeRepository.findById(id);
        if (cvOpt.isEmpty()) {
            return new ApiResponse<>("KhÃ´ng tÃ¬m tháº¥y Resume", null, HttpStatus.NOT_FOUND.value());
        }
        Resume cv = cvOpt.get();
        if (cv.getUser() == null || !principal.getName().equals(cv.getUser().getEmail())) {
            return new ApiResponse<>("KhÃ´ng cÃ³ quyá»n xÃ³a", null, HttpStatus.FORBIDDEN.value());
        }
        resumeRepository.delete(cv);
        return new ApiResponse<>("ThÃ nh cÃ´ng", null, HttpStatus.OK.value());
    }
}





