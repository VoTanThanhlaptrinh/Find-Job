package com.job_web.application_process.application.impl;

import com.job_web.application_process.domain.repository.ResumeRepository;
import com.job_web.application_process.infrastructure.query.ResumeDSL;
import com.job_web.application_process.infrastructure.ai.dto.ResumeParsingMessage;
import com.job_web.application_process.api.dto.ResumeDTO;
import com.job_web.application_process.api.dto.ResumeDetailDTO;
import com.job_web.application_process.api.dto.ResumeUploadDTO;
import com.job_web.application_process.api.dto.ResumeUrlDTO;
import com.job_web.shared.api.message.dto.CloudUploadMessage;
import com.job_web.application_process.api.dto.ResumeView;
import com.job_web.shared.domain.exception.ForbiddenException;
import com.job_web.shared.domain.exception.ResourceNotFoundException;
import com.job_web.shared.domain.exception.UnauthorizedException;
import com.job_web.shared.infrastructure.message.MessageProducer;
import com.job_web.application_process.domain.model.Resume;
import com.job_web.identity.domain.model.User;
import com.job_web.application_process.application.AIService;
import com.job_web.application_process.application.ResumeService;
import com.job_web.shared.application.FileService;
import com.job_web.shared.application.S3PresignedUrlService;
import com.job_web.shared.utils.KeyGeneratorUtil;
import com.job_web.shared.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
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

    private static final String MDC_USER_ID = "userId";
    private static final String MDC_CV_ID = "cvId";

    @Override
    public List<ResumeView> getListResumeOfUser(User currentUser) {
        var resumes = resumeDSL.getListResumeOfUser(currentUser != null ? currentUser.getEmail() : "");
        if(resumes.isEmpty()){
            throw new ResourceNotFoundException(MessageUtils.getMessage("resume.not_found"));
        }

        return resumes;
    }

    @Override
    public List<ResumeDTO> getResumesByUser(String email) {
        throw new NotImplementedException(MessageUtils.getMessage("resume.not_implemented"));
    }

    @Override
    public ResumeDetailDTO getResumeDetail(long id, User user) {

        if (user == null) {
            throw new UnauthorizedException(MessageUtils.getMessage("message.unauthorized"));
        }
        Optional<Resume> cvOpt = resumeRepository.findById(id);
        if (cvOpt.isEmpty()) {
            throw new ResourceNotFoundException(MessageUtils.getMessage("resume.not_found"));
        }
        Resume cv = cvOpt.get();
        if (cv.getUser() == null || !user.getEmail().equals(cv.getUser().getEmail())) {
            throw new ForbiddenException(MessageUtils.getMessage("resume.access.forbidden"));
        }
        return new ResumeDetailDTO(cv.getId(), cv.getFileName(), cv.getCreateDate());
    }

    @Override
    public ResumeView createResume(ResumeUploadDTO resumeUploadDTO, User user) {
        if (user == null) {
            throw new UnauthorizedException(MessageUtils.getMessage("message.unauthorized"));
        }

        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));

            log.info("Creating resume for user: {}, file: {}",
                    user.getId(), resumeUploadDTO.getFile().getOriginalFilename());

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
                if (rawText == null || rawText.isEmpty()) {
                    log.info("Standard text extraction yielded empty result for user: {} — falling back to OCR", user.getId());
                    rawText = fileService.extractTextFromFileOcr(resumeUploadDTO.getFile());
                }
                rawText = fileService.cleanText(rawText);
            } catch (Exception e) {
                log.warn("File processing failed during resume creation for user: {}", user.getId());
                throw new RuntimeException(MessageUtils.getMessage("resume.upload.failed"));
            }
            if (data.length == 0) {
                log.warn("Empty file uploaded during resume creation for user: {}", user.getId());
                throw new RuntimeException(MessageUtils.getMessage("message.error"));
            }

            producer.uploadToCloud(new CloudUploadMessage(data, key, resumeUploadDTO.getFile().getOriginalFilename()));
            resumeRepository.save(cv);
            MDC.put(MDC_CV_ID, String.valueOf(cv.getId()));

            producer.processAI(new ResumeParsingMessage(rawText, user.getId(), cv.getId()));

            log.info("Resume created — cv: {}, dispatched cloud upload and AI processing for user: {}",
                    cv.getId(), user.getId());

            return new ResumeView(cv.getId(), cv.getFileName(), cv.getCreateDate());
        } finally {
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_CV_ID);
        }
    }

    @Override
    public void updateResume(long id, ResumeUploadDTO resumeUploadDTO, User user) {
        if (user == null) {
            throw new UnauthorizedException(MessageUtils.getMessage("message.unauthorized"));
        }

        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));
            MDC.put(MDC_CV_ID, String.valueOf(id));

            log.info("Updating resume: {} for user: {}", id, user.getId());

            Optional<Resume> cvOpt = resumeRepository.findById(id);
            if (cvOpt.isEmpty()) {
                throw new ResourceNotFoundException(MessageUtils.getMessage("resume.not_found"));
            }
            Resume cv = cvOpt.get();
            if (cv.getUser() == null || !user.getEmail().equals(cv.getUser().getEmail())) {
                log.warn("Resume update forbidden — user: {} does not own CV: {}", user.getId(), id);
                throw new ForbiddenException(MessageUtils.getMessage("resume.edit.forbidden"));
            }

            cv.setFileName(resumeUploadDTO.getFile().getOriginalFilename());
            String key = cv.getKeyCf();
            byte[] data;
            try {
                data = toByteArray(resumeUploadDTO.getFile().getInputStream());
            } catch (Exception e) {
                log.warn("File processing failed during resume update for CV: {}", id);
                throw new RuntimeException(MessageUtils.getMessage("resume.upload.failed"));
            }
            if (data.length == 0) {
                log.warn("Empty file uploaded during resume update for CV: {}", id);
                throw new RuntimeException(MessageUtils.getMessage("message.error"));
            }

            producer.uploadToCloud(new CloudUploadMessage(data, key, resumeUploadDTO.getFile().getOriginalFilename()));
            resumeRepository.save(cv);

            log.info("Resume updated — cv: {}, dispatched cloud upload for user: {}", id, user.getId());
        } finally {
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_CV_ID);
        }
    }

    @Override
    public void deleteResume(long id, User user) {
        if (user == null) {
            throw new UnauthorizedException(MessageUtils.getMessage("message.unauthorized"));
        }

        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));
            MDC.put(MDC_CV_ID, String.valueOf(id));

            Optional<Resume> cvOpt = resumeRepository.findById(id);
            if (cvOpt.isEmpty()) {
                throw new ResourceNotFoundException(MessageUtils.getMessage("resume.not_found"));
            }
            Resume cv = cvOpt.get();
            if (cv.getUser() == null || !user.getEmail().equals(cv.getUser().getEmail())) {
                log.warn("Resume delete forbidden — user: {} does not own CV: {}", user.getId(), id);
                throw new ForbiddenException(MessageUtils.getMessage("resume.delete.forbidden"));
            }

            cv.markDeleted();
            resumeRepository.save(cv);
            log.info("Resume soft-deleted — cv: {} by user: {}", id, user.getId());
        } finally {
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_CV_ID);
        }
    }

    @Override
    public void uploadResumeToCloud(byte[] data, String key, String originalName) {
        log.info("Uploading resume to cloud storage — key: {}, size: {} bytes", key, data.length);

        String contentType = determineContentType(originalName);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(data));

        log.info("Resume uploaded to cloud storage — key: {}", key);
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
    public ResumeUrlDTO getResumeViewUrl(long id, User user) {
        if (user == null) {
            throw new UnauthorizedException(MessageUtils.getMessage("message.unauthorized"));
        }
        Optional<Resume> cvOpt = resumeRepository.findById(id);
        if (cvOpt.isEmpty()) {
            throw new ResourceNotFoundException(MessageUtils.getMessage("resume.not_found"));
        }
        Resume cv = cvOpt.get();
        if (cv.getUser() == null || !user.getEmail().equals(cv.getUser().getEmail())) {
            throw new ForbiddenException(MessageUtils.getMessage("resume.view.forbidden"));
        }
        try {
            String url = s3PresignedUrlService.generateViewUrl(cv.getKeyCf(), DEFAULT_URL_EXPIRATION_MINUTES);
            return new ResumeUrlDTO(cv.getId(), cv.getFileName(), url, DEFAULT_URL_EXPIRATION_MINUTES);
        } catch (Exception e) {
            log.warn("Failed to generate view URL for CV: {}, user: {}", id, user.getId());
            throw new RuntimeException(MessageUtils.getMessage("resume.url.generate_failed"));
        }
    }

    @Override
    public ResumeUrlDTO getResumeDownloadUrl(long id, User user) {
        if (user == null) {
            throw new UnauthorizedException(MessageUtils.getMessage("message.unauthorized"));
        }
        Optional<Resume> cvOpt = resumeRepository.findById(id);
        if (cvOpt.isEmpty()) {
            throw new ResourceNotFoundException(MessageUtils.getMessage("resume.not_found"));
        }
        Resume cv = cvOpt.get();
        if (cv.getUser() == null || !user.getEmail().equals(cv.getUser().getEmail())) {
            throw new ForbiddenException(MessageUtils.getMessage("resume.download.forbidden"));
        }

        try {
            String url = s3PresignedUrlService.generateDownloadUrl(cv.getKeyCf(), cv.getFileName(), DEFAULT_URL_EXPIRATION_MINUTES);
            return new ResumeUrlDTO(cv.getId(), cv.getFileName(), url, DEFAULT_URL_EXPIRATION_MINUTES);
        } catch (Exception e) {
            log.warn("Failed to generate download URL for CV: {}, user: {}", id, user.getId());
            throw new RuntimeException(MessageUtils.getMessage("resume.url.generate_failed"));
        }
    }

    @Override
    public ResumeUrlDTO getResumeViewUrlForHirer(long id) {
        Optional<Resume> cvOpt = resumeRepository.findById(id);
        if (cvOpt.isEmpty()) {
            throw new ResourceNotFoundException(MessageUtils.getMessage("resume.not_found"));
        }
        Resume cv = cvOpt.get();

        try {
            String url = s3PresignedUrlService.generateViewUrl(cv.getKeyCf(), DEFAULT_URL_EXPIRATION_MINUTES);
            return new ResumeUrlDTO(cv.getId(), cv.getFileName(), url, DEFAULT_URL_EXPIRATION_MINUTES);
        } catch (Exception e) {
            log.warn("Failed to generate hirer view URL for CV: {}", id);
            throw new RuntimeException(MessageUtils.getMessage("resume.url.generate_failed"));
        }
    }

    @Override
    public ResumeUrlDTO getResumeDownloadUrlForHirer(long id) {
        Optional<Resume> cvOpt = resumeRepository.findById(id);
        if (cvOpt.isEmpty()) {
            throw new ResourceNotFoundException(MessageUtils.getMessage("resume.not_found"));
        }
        Resume cv = cvOpt.get();

        try {
            String url = s3PresignedUrlService.generateDownloadUrl(cv.getKeyCf(), cv.getFileName(), DEFAULT_URL_EXPIRATION_MINUTES);
            return new ResumeUrlDTO(cv.getId(), cv.getFileName(), url, DEFAULT_URL_EXPIRATION_MINUTES);
        } catch (Exception e) {
            log.warn("Failed to generate hirer download URL for CV: {}", id);
            throw new RuntimeException(MessageUtils.getMessage("resume.url.generate_failed"));
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
