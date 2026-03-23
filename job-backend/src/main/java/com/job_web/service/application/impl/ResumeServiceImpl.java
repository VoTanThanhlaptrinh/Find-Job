package com.job_web.service.application.impl;

import com.job_web.data.ResumeRepository;
import com.job_web.data.UserRepository;
import com.job_web.dto.application.ResumeDTO;
import com.job_web.dto.application.ResumeDetailDTO;
import com.job_web.dto.application.ResumeUploadDTO;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.application.ResumeView;
import com.job_web.models.Resume;
import com.job_web.models.User;
import com.job_web.service.application.ResumeService;
import com.job_web.utills.KeyGeneratorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final S3Client s3Client;

    @Value("${cloudflare.r2.bucket-name}")
    private String bucketName;
    @Override
    public ApiResponse<List<ResumeView>> getListResumeOfUser(Principal principal) {
        User user = new User();
        user.setEmail(principal.getName());
        Resume resume = new Resume();
        resume.setUser(user);
        Example<Resume> example = Example.of(resume);
        var resumes = resumeRepository.findBy(example, query ->
                query.as(ResumeView.class)
                        .sortBy(Sort.by("createDate").descending())
                        .all());
        String message = resumes.isEmpty() ? "The user has not uploaded any resumes yet." : "success";
        return new ApiResponse<>(message,resumes, HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<List<ResumeDTO>> getResumesByUser(String email) {
        return new ApiResponse<>("Not implemented.", null, HttpStatus.NOT_IMPLEMENTED.value());
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
        String key = KeyGeneratorUtil.generateKey();
        cv.setKey(key);
        byte[] data;
        try{
            data = toByteArray(resumeUploadDTO.getFile().getInputStream());
        }catch (Exception e){
            return new ApiResponse<>("faild", null, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        if(data.length == 0){
            return new ApiResponse<>("error", null, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        uploadResumeToCloud(data, key);
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
        String key = cv.getKey();
        byte[] data;
        try{
            data = toByteArray(resumeUploadDTO.getFile().getInputStream());
        }catch (Exception e){
            return new ApiResponse<>("faild", null, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        if(data.length == 0){
            return new ApiResponse<>("error", null, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        uploadResumeToCloud(data, key);
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
        cv.markDeleted();
        resumeRepository.save(cv);
        return new ApiResponse<>("success", null, HttpStatus.OK.value());
    }

    @Override
    public void uploadResumeToCloud(byte[] data, String key) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName) // Chỉ định đúng bucket thứ 2 của bạn ở đây
                .key(key)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(data));
    }

    @Override
    public byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[8192]; // Tạo mảng đệm 8KB

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }
}
