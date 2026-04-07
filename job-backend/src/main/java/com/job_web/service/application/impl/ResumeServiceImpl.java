package com.job_web.service.application.impl;

import com.job_web.data.ResumeRepository;
import com.job_web.data.queryDSL.ResumeDSL;
import com.job_web.dto.ai.ResumeParsingMessage;
import com.job_web.dto.application.ResumeDTO;
import com.job_web.dto.application.ResumeDetailDTO;
import com.job_web.dto.application.ResumeUploadDTO;
import com.job_web.dto.application.ResumeUrlDTO;
import com.job_web.dto.message.CloudUploadMessage;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.application.ResumeView;
import com.job_web.message.MessageProducer;
import com.job_web.models.Resume;
import com.job_web.models.User;
import com.job_web.service.ai.AIService;
import com.job_web.service.application.ResumeService;
import com.job_web.service.support.FileService;
import com.job_web.service.support.S3PresignedUrlService;
import com.job_web.utills.KeyGeneratorUtil;
import com.job_web.utills.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {
    private final ResumeRepository resumeRepository;
    private final ResumeDSL resumeDSL;
    private final S3Client s3Client;
    private final FileService fileService;
    private final MessageProducer producer;
    private final AIService aiService;
    private final S3PresignedUrlService s3PresignedUrlService;

    @Value("${cloudflare.r2.bucket-name}")
    private String bucketName;

    private static final int DEFAULT_URL_EXPIRATION_MINUTES = 30;

    @Override
    public ApiResponse<List<ResumeView>> getListResumeOfUser(User user) {
        var resumes = resumeDSL.getListResumeOfUser(user != null ? user.getEmail() : "");
        String message = resumes.isEmpty() ? MessageUtils.getMessage("resume.not_found") : MessageUtils.getMessage("message.success");
        return new ApiResponse<>(message, resumes, HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<List<ResumeDTO>> getResumesByUser(String email) {
        return new ApiResponse<>(MessageUtils.getMessage("resume.not_implemented"), null, HttpStatus.NOT_IMPLEMENTED.value());
    }

    @Override
    public ApiResponse<ResumeDetailDTO> getResumeDetail(long id, User user) {
        if (user == null) {
            return new ApiResponse<>(MessageUtils.getMessage("message.unauthorized"), null, HttpStatus.UNAUTHORIZED.value());
        }
        Optional<Resume> cvOpt = resumeRepository.findById(id);
        if (cvOpt.isEmpty()) {
            return new ApiResponse<>(MessageUtils.getMessage("resume.not_found"), null, HttpStatus.NOT_FOUND.value());
        }
        Resume cv = cvOpt.get();
        if (cv.getUser() == null || !user.getEmail().equals(cv.getUser().getEmail())) {
            return new ApiResponse<>(MessageUtils.getMessage("resume.access.forbidden"), null, HttpStatus.FORBIDDEN.value());
        }
        ResumeDetailDTO detail = new ResumeDetailDTO(cv.getId(), cv.getFileName(), cv.getCreateDate());
        return new ApiResponse<>(MessageUtils.getMessage("message.success"), detail, HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<ResumeView> createResume(ResumeUploadDTO resumeUploadDTO, User user) {
        if (user == null) {
            return new ApiResponse<>(MessageUtils.getMessage("message.unauthorized"), null, HttpStatus.UNAUTHORIZED.value());
        }
        Resume cv = new Resume();
        cv.setUser(user);
        cv.setFileName(resumeUploadDTO.getFile().getOriginalFilename());
        String key = KeyGeneratorUtil.generateKey();
        cv.setKeyCf(key);
        byte[] data;
        String rawText;
        try {
            data = toByteArray(resumeUploadDTO.getFile().getInputStream());
            rawText = fileService.extractTextFromFile(resumeUploadDTO.getFile().getInputStream());
            if(rawText == null || rawText.isEmpty()){
                rawText = fileService.extractTextFromFileOcr(resumeUploadDTO.getFile());
            }
            rawText = fileService.cleanText(rawText);
        } catch (Exception e) {
            return new ApiResponse<>(MessageUtils.getMessage("resume.upload.failed"), null, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        if (data.length == 0) {
            return new ApiResponse<>(MessageUtils.getMessage("message.error"), null, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        producer.uploadToCloud(new CloudUploadMessage(data, key, resumeUploadDTO.getFile().getOriginalFilename()));
        resumeRepository.save(cv);
        producer.processAI(new ResumeParsingMessage(rawText, user.getId(), cv.getId()));
        return new ApiResponse<>(MessageUtils.getMessage("message.success"), new ResumeView(cv.getId(), cv.getFileName(), cv.getCreateDate()), HttpStatus.CREATED.value());
    }

    @Override
    public ApiResponse<String> updateResume(long id, ResumeUploadDTO resumeUploadDTO, User user) {
        if (user == null) {
            return new ApiResponse<>(MessageUtils.getMessage("message.unauthorized"), null, HttpStatus.UNAUTHORIZED.value());
        }
        Optional<Resume> cvOpt = resumeRepository.findById(id);
        if (cvOpt.isEmpty()) {
            return new ApiResponse<>(MessageUtils.getMessage("resume.not_found"), null, HttpStatus.NOT_FOUND.value());
        }
        Resume cv = cvOpt.get();
        if (cv.getUser() == null || !user.getEmail().equals(cv.getUser().getEmail())) {
            return new ApiResponse<>(MessageUtils.getMessage("resume.edit.forbidden"), null, HttpStatus.FORBIDDEN.value());
        }
        cv.setFileName(resumeUploadDTO.getFile().getOriginalFilename());
        String key = cv.getKeyCf();
        byte[] data;
        try {
            data = toByteArray(resumeUploadDTO.getFile().getInputStream());
        } catch (Exception e) {
            return new ApiResponse<>(MessageUtils.getMessage("resume.upload.failed"), null, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        if (data.length == 0) {
            return new ApiResponse<>(MessageUtils.getMessage("message.error"), null, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        producer.uploadToCloud(new CloudUploadMessage(data, key, resumeUploadDTO.getFile().getOriginalFilename()));
        resumeRepository.save(cv);
        return new ApiResponse<>(MessageUtils.getMessage("message.success"), null, HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<String> deleteResume(long id, User user) {
        if (user == null) {
            return new ApiResponse<>(MessageUtils.getMessage("message.unauthorized"), null, HttpStatus.UNAUTHORIZED.value());
        }
        Optional<Resume> cvOpt = resumeRepository.findById(id);
        if (cvOpt.isEmpty()) {
            return new ApiResponse<>(MessageUtils.getMessage("resume.not_found"), null, HttpStatus.NOT_FOUND.value());
        }
        Resume cv = cvOpt.get();
        if (cv.getUser() == null || !user.getEmail().equals(cv.getUser().getEmail())) {
            return new ApiResponse<>(MessageUtils.getMessage("resume.delete.forbidden"), null, HttpStatus.FORBIDDEN.value());
        }
        cv.markDeleted();
        resumeRepository.save(cv);
        return new ApiResponse<>(MessageUtils.getMessage("message.success"), null, HttpStatus.OK.value());
    }

    @Override
    public void uploadResumeToCloud(byte[] data, String key, String originalName) {
        String contentType = determineContentType(originalName);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(data));
    }

    @Override
    public byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[8192];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    @Override
    public ApiResponse<ResumeUrlDTO> getResumeViewUrl(long id, User user) {
        if (user == null) {
            return new ApiResponse<>(MessageUtils.getMessage("message.unauthorized"), null, HttpStatus.UNAUTHORIZED.value());
        }
        Optional<Resume> cvOpt = resumeRepository.findById(id);
        if (cvOpt.isEmpty()) {
            return new ApiResponse<>(MessageUtils.getMessage("resume.not_found"), null, HttpStatus.NOT_FOUND.value());
        }
        Resume cv = cvOpt.get();
        if (cv.getUser() == null || !user.getEmail().equals(cv.getUser().getEmail())) {
            return new ApiResponse<>(MessageUtils.getMessage("resume.view.forbidden"), null, HttpStatus.FORBIDDEN.value());
        }

        try {
            String url = s3PresignedUrlService.generateViewUrl(cv.getKeyCf(), DEFAULT_URL_EXPIRATION_MINUTES);
            ResumeUrlDTO urlDTO = new ResumeUrlDTO(cv.getId(), cv.getFileName(), url, DEFAULT_URL_EXPIRATION_MINUTES);
            return new ApiResponse<>(MessageUtils.getMessage("message.success"), urlDTO, HttpStatus.OK.value());
        } catch (Exception e) {
            log.error("Error generating view URL for resume id: {}", id, e);
            return new ApiResponse<>(MessageUtils.getMessage("resume.url.generate_failed"), null, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Override
    public ApiResponse<ResumeUrlDTO> getResumeDownloadUrl(long id, User user) {
        if (user == null) {
            return new ApiResponse<>(MessageUtils.getMessage("message.unauthorized"), null, HttpStatus.UNAUTHORIZED.value());
        }
        Optional<Resume> cvOpt = resumeRepository.findById(id);
        if (cvOpt.isEmpty()) {
            return new ApiResponse<>(MessageUtils.getMessage("resume.not_found"), null, HttpStatus.NOT_FOUND.value());
        }
        Resume cv = cvOpt.get();
        if (cv.getUser() == null || !user.getEmail().equals(cv.getUser().getEmail())) {
            return new ApiResponse<>(MessageUtils.getMessage("resume.download.forbidden"), null, HttpStatus.FORBIDDEN.value());
        }

        try {
            String url = s3PresignedUrlService.generateDownloadUrl(cv.getKeyCf(), cv.getFileName(), DEFAULT_URL_EXPIRATION_MINUTES);
            ResumeUrlDTO urlDTO = new ResumeUrlDTO(cv.getId(), cv.getFileName(), url, DEFAULT_URL_EXPIRATION_MINUTES);
            return new ApiResponse<>(MessageUtils.getMessage("message.success"), urlDTO, HttpStatus.OK.value());
        } catch (Exception e) {
            log.error("Error generating download URL for resume id: {}", id, e);
            return new ApiResponse<>(MessageUtils.getMessage("resume.url.generate_failed"), null, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Override
    public ApiResponse<ResumeUrlDTO> getResumeViewUrlForHirer(long id) {
        Optional<Resume> cvOpt = resumeRepository.findById(id);
        if (cvOpt.isEmpty()) {
            return new ApiResponse<>(MessageUtils.getMessage("resume.not_found"), null, HttpStatus.NOT_FOUND.value());
        }
        Resume cv = cvOpt.get();

        try {
            String url = s3PresignedUrlService.generateViewUrl(cv.getKeyCf(), DEFAULT_URL_EXPIRATION_MINUTES);
            ResumeUrlDTO urlDTO = new ResumeUrlDTO(cv.getId(), cv.getFileName(), url, DEFAULT_URL_EXPIRATION_MINUTES);
            return new ApiResponse<>(MessageUtils.getMessage("message.success"), urlDTO, HttpStatus.OK.value());
        } catch (Exception e) {
            log.error("Error generating view URL for resume id: {}", id, e);
            return new ApiResponse<>(MessageUtils.getMessage("resume.url.generate_failed"), null, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    @Override
    public ApiResponse<ResumeUrlDTO> getResumeDownloadUrlForHirer(long id) {
        Optional<Resume> cvOpt = resumeRepository.findById(id);
        if (cvOpt.isEmpty()) {
            return new ApiResponse<>(MessageUtils.getMessage("resume.not_found"), null, HttpStatus.NOT_FOUND.value());
        }
        Resume cv = cvOpt.get();

        try {
            String url = s3PresignedUrlService.generateDownloadUrl(cv.getKeyCf(), cv.getFileName(), DEFAULT_URL_EXPIRATION_MINUTES);
            ResumeUrlDTO urlDTO = new ResumeUrlDTO(cv.getId(), cv.getFileName(), url, DEFAULT_URL_EXPIRATION_MINUTES);
            return new ApiResponse<>(MessageUtils.getMessage("message.success"), urlDTO, HttpStatus.OK.value());
        } catch (Exception e) {
            log.error("Error generating download URL for resume id: {}", id, e);
            return new ApiResponse<>(MessageUtils.getMessage("resume.url.generate_failed"), null, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    private String determineContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";

        String lowerCaseName = fileName.toLowerCase();
        if (lowerCaseName.endsWith(".pdf")) return "application/pdf";
        if (lowerCaseName.endsWith(".doc")) return "application/msword";
        if (lowerCaseName.endsWith(".docx"))
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lowerCaseName.endsWith(".png")) return "image/png";
        if (lowerCaseName.endsWith(".jpg") || lowerCaseName.endsWith(".jpeg")) return "image/jpeg";

        return "application/octet-stream";
    }
}
