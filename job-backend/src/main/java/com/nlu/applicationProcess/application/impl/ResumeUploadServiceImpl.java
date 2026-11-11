package com.nlu.applicationProcess.application.impl;

import com.nlu.applicationProcess.api.dto.req.ResumeUploadInitiateRequest;
import com.nlu.applicationProcess.api.dto.req.ResumeView;
import com.nlu.applicationProcess.api.dto.res.ResumeUploadInitiateResponse;
import com.nlu.applicationProcess.application.ResumeUploadService;
import com.nlu.applicationProcess.domain.model.ClaimResult;
import com.nlu.applicationProcess.domain.model.Resume;
import com.nlu.applicationProcess.domain.model.ResumeUploadSession;
import com.nlu.applicationProcess.domain.model.ResumeUploadSessionStatus;
import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.applicationProcess.domain.repository.ResumeUploadSessionRepository;
import com.nlu.applicationProcess.infrastructure.redis.ResumeUploadLockService;
import com.nlu.identity.domain.model.User;
import com.nlu.shared.application.FileServiceClient;
import com.nlu.shared.domain.exception.*;
import com.nlu.shared.infrastructure.config.ResumeUploadProperties;
import com.nlu.shared.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeUploadServiceImpl implements ResumeUploadService {

    private final ResumeUploadSessionRepository sessionRepository;
    private final ResumeRepository resumeRepository;
    private final FileServiceClient fileServiceClient;
    private final ResumeUploadFinalizer resumeUploadFinalizer;
    private final ResumeUploadLockService lockService;
    private final ResumeUploadProperties properties;

    private static final Map<String, String> ALLOWED_EXTENSIONS_TO_MIME = Map.of(
            ".pdf", "application/pdf",
            ".doc", "application/msword",
            ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    @Override
    public ResumeUploadInitiateResponse initiateUpload(String idempotencyKey, ResumeUploadInitiateRequest request,
            User currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedException(MessageUtils.getMessage("message.unauthorized"));
        }

        // Validate idempotency key
        String trimmedKey = validateAndNormalizeIdempotencyKey(idempotencyKey);

        // Validate size
        if (request.size() == null || request.size() <= 0) {
            throw new BadRequestException("File size must be greater than 0");
        }

        if (request.size() > properties.getMaxFileSizeBytes()) {
            throw new BadRequestException(MessageUtils.getMessage("resume.file_size_exceeded"));
        }

        // Validate and sanitize filename
        String sanitizedFileName = sanitizeFileName(request.fileName());

        // Validate extension and strict matching with content type
        validateFileTypeAndExtension(sanitizedFileName, request.contentType());

        // Check resume quota
        if (resumeRepository.countResumesByUser_Id(currentUser.getId()) >= 100) {
            throw new BadRequestException(MessageUtils.getMessage("resume.limit_exceeded"));
        }

        String normalizedContentType = normalizeContentType(request.contentType());
        String fingerprint = calculateFingerprint(sanitizedFileName, request.size(), normalizedContentType);

        // Try Redis lock for short-term double click prevention
        String lockToken = UUID.randomUUID().toString();
        Optional<String> acquiredLockOpt = lockService.acquireLock(currentUser.getId(), trimmedKey, lockToken);

        try {
            // Check if session already exists in DB
            Optional<ResumeUploadSession> existingSessionOpt = sessionRepository.findByUserIdAndIdempotencyKey(
                    currentUser.getId(), trimmedKey);

            if (existingSessionOpt.isPresent()) {
                return handleExistingSession(existingSessionOpt.get(), fingerprint);
            }

            // Create new upload session
            UUID uploadId = UUID.randomUUID();
            var remotePresign = fileServiceClient.presign(sanitizedFileName, normalizedContentType, request.size());
            validatePresignResponse(remotePresign.tempKey(), remotePresign.uploadUrl());
            String tempKey = remotePresign.tempKey();
            String permanentKey = tempKey.substring("temp/".length());
            LocalDateTime uploadUrlExpiresAt = LocalDateTime.now().plusSeconds(remotePresign.expiresIn());

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime sessionExpiresAt = now.plusMinutes(properties.getSessionExpirationMinutes());

            ResumeUploadSession session = ResumeUploadSession.builder()
                    .id(uploadId)
                    .userId(currentUser.getId())
                    .idempotencyKey(trimmedKey)
                    .requestFingerprint(fingerprint)
                    .originalFileName(sanitizedFileName)
                    .declaredContentType(normalizedContentType)
                    .declaredSize(request.size())
                    .tempKey(tempKey)
                    .permanentKey(permanentKey)
                    .uploadUrl(remotePresign.uploadUrl())
                    .uploadUrlExpiresAt(uploadUrlExpiresAt)
                    .status(ResumeUploadSessionStatus.PENDING_UPLOAD)
                    .resumeId(null)
                    .cleanupAttempts(0)
                    .expiresAt(sessionExpiresAt)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            try {
                sessionRepository.save(session);
                log.info("Created ResumeUploadSession id: {} for user: {} with idempotencyKey: {}",
                        uploadId, currentUser.getId(), trimmedKey);
            } catch (DataIntegrityViolationException dive) {
                // Concurrency fallback when Redis lock was bypassed or unavailable
                log.info("Concurrent insert race detected for user {} and key {}. Reading existing record.",
                        currentUser.getId(), trimmedKey);
                ResumeUploadSession racedSession = sessionRepository
                        .findByUserIdAndIdempotencyKey(currentUser.getId(), trimmedKey)
                        .orElseThrow(() -> dive);
                return handleExistingSession(racedSession, fingerprint);
            }

            return new ResumeUploadInitiateResponse(
                    uploadId,
                    remotePresign.uploadUrl(),
                    "PUT",
                    Map.of("Content-Type", normalizedContentType),
                    uploadUrlExpiresAt);
        } finally {
            if (acquiredLockOpt.isPresent()) {
                lockService.releaseLock(currentUser.getId(), trimmedKey, lockToken);
            }
        }
    }

    private ResumeUploadInitiateResponse handleExistingSession(ResumeUploadSession existingSession,
            String requestFingerprint) {
        // Enforce fingerprint match
        if (!Objects.equals(existingSession.getRequestFingerprint(), requestFingerprint)) {
            log.warn("Idempotency conflict for user {} and key {}. Stored fingerprint: {}, Request: {}",
                    existingSession.getUserId(), existingSession.getIdempotencyKey(),
                    existingSession.getRequestFingerprint(), requestFingerprint);
            throw new ConflictException("Idempotency key has already been used with different request parameters");
        }

        log.info("Idempotent initiate request for session {}. Current status: {}", existingSession.getId(),
                existingSession.getStatus());

        switch (existingSession.getStatus()) {
            case PENDING_UPLOAD -> {
                if (LocalDateTime.now().isAfter(existingSession.getExpiresAt())) {
                    throw new BadRequestException("Upload session has expired: " + existingSession.getId());
                }
                if (existingSession.getUploadUrl() != null
                        && existingSession.getUploadUrlExpiresAt() != null
                        && LocalDateTime.now().isBefore(existingSession.getUploadUrlExpiresAt().minusSeconds(15))) {
                    return new ResumeUploadInitiateResponse(
                            existingSession.getId(),
                            existingSession.getUploadUrl(),
                            "PUT",
                            Map.of("Content-Type", existingSession.getDeclaredContentType()),
                            existingSession.getUploadUrlExpiresAt());
                }
                // The file service generates a fresh temp key for each presign.
                // Rotate the session key only while it is still awaiting upload.
                var remotePresign = fileServiceClient.presign(
                        existingSession.getOriginalFileName(),
                        existingSession.getDeclaredContentType(),
                        existingSession.getDeclaredSize());
                validatePresignResponse(remotePresign.tempKey(), remotePresign.uploadUrl());
                existingSession.setTempKey(remotePresign.tempKey());
                existingSession.setPermanentKey(remotePresign.tempKey().substring("temp/".length()));
                existingSession.setUploadUrl(remotePresign.uploadUrl());
                existingSession.setUploadUrlExpiresAt(LocalDateTime.now().plusSeconds(remotePresign.expiresIn()));
                sessionRepository.save(existingSession);
                return new ResumeUploadInitiateResponse(
                        existingSession.getId(),
                        remotePresign.uploadUrl(),
                        "PUT",
                        Map.of("Content-Type", existingSession.getDeclaredContentType()),
                        existingSession.getUploadUrlExpiresAt());
            }
            case FINALIZING -> {
                throw new ConflictException(
                        "Upload session is currently finalizing. Please check status or retry complete.");
            }
            case COMPLETED -> {
                log.info("Upload session {} already COMPLETED. Returning existing resume id: {}",
                        existingSession.getId(), existingSession.getResumeId());
                return new ResumeUploadInitiateResponse(
                        existingSession.getId(),
                        null,
                        null,
                        Map.of(),
                        existingSession.getExpiresAt());
            }
            case REJECTED -> {
                String code = existingSession.getRejectionCode() != null ? existingSession.getRejectionCode()
                        : "UPLOAD_REJECTED";
                String detail = existingSession.getRejectionDetail() != null ? existingSession.getRejectionDetail()
                        : "Upload session was rejected";
                throw new BadRequestException("Upload session was rejected: " + code + " - " + detail);
            }
            case EXPIRED -> {
                throw new BadRequestException("Upload session has expired: " + existingSession.getId());
            }
            default -> throw new BadRequestException("Unknown session status: " + existingSession.getStatus());
        }
    }

    @Override
    public ResumeView completeUpload(UUID uploadId, User currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedException(MessageUtils.getMessage("message.unauthorized"));
        }

        // Claim session in short database transaction: PENDING_UPLOAD -> FINALIZING
        ClaimResult claimResult = resumeUploadFinalizer.claimForFinalizing(uploadId, currentUser);

        if (claimResult.status() == ClaimResult.ClaimStatus.ALREADY_COMPLETED) {
            log.info("Session {} is already COMPLETED. Returning existing resume id: {}",
                    uploadId, claimResult.existingResumeId());
            Resume resume = resumeRepository.findById(claimResult.existingResumeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Associated resume not found: " + claimResult.existingResumeId()));
            return new ResumeView(resume.getId(), resume.getFileName(), resume.getCreatedAt(), resume.getStatus());
        }

        if (claimResult.status() == ClaimResult.ClaimStatus.IN_PROGRESS) {
            log.info("Session {} is currently FINALIZING. Waiting for completion...", uploadId);
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                Optional<ResumeUploadSession> pollOpt = sessionRepository.findById(uploadId);
                if (pollOpt.isEmpty()) {
                    continue;
                }
                ResumeUploadSession polled = pollOpt.get();
                if (polled.getStatus() == ResumeUploadSessionStatus.COMPLETED) {
                    Resume resume = resumeRepository.findById(polled.getResumeId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Associated resume not found: " + polled.getResumeId()));
                    return new ResumeView(resume.getId(), resume.getFileName(), resume.getCreatedAt(), resume.getStatus());
                }
                if (polled.getStatus() == ResumeUploadSessionStatus.REJECTED) {
                    throw new BadRequestException("Upload session was rejected: " + uploadId);
                }
                if (polled.getStatus() == ResumeUploadSessionStatus.EXPIRED) {
                    throw new BadRequestException("Upload session has expired: " + uploadId);
                }
            }
            throw new ConflictException("Upload finalization is in progress. Please retry shortly.");
        }

        String processingToken = claimResult.processingToken();
        confirmTempObject(uploadId, processingToken, claimResult.session());
        Resume finalizedResume = resumeUploadFinalizer.finalizeUpload(uploadId, processingToken, currentUser);

        return new ResumeView(
                finalizedResume.getId(),
                finalizedResume.getFileName(),
                finalizedResume.getCreatedAt(),
                finalizedResume.getStatus());
    }

    private void confirmTempObject(UUID uploadId, String processingToken, ResumeUploadSession session) {
        try {
            var confirmed = fileServiceClient.confirm(session.getTempKey());
            if (confirmed == null || confirmed.key() == null || !confirmed.key().equals(session.getPermanentKey())) {
                resumeUploadFinalizer.releaseFinalizingClaim(uploadId, processingToken);
                throw new StorageException("File service returned an unexpected permanent key");
            }
        } catch (HttpClientErrorException ex) {
            String detail = ex.getMessage();
            if (detail != null && detail.length() > 512) {
                detail = detail.substring(0, 512);
            }
            resumeUploadFinalizer.markRejected(
                    uploadId,
                    processingToken,
                    "TEMP_CONFIRM_REJECTED",
                    detail != null ? detail : "Temp file rejected by storage service",
                    null,
                    null,
                    null);
            throw new BadRequestException("Uploaded file not found or rejected in temporary storage");
        } catch (StorageException ex) {
            throw ex;
        } catch (Exception ex) {
            resumeUploadFinalizer.releaseFinalizingClaim(uploadId, processingToken);
            throw new StorageException("Failed to confirm uploaded file in storage", ex);
        }
    }

    private String validateAndNormalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required");
        }
        String trimmed = idempotencyKey.trim();
        if (trimmed.length() > 128) {
            throw new BadRequestException("Idempotency-Key must not exceed 128 characters");
        }
        try {
            UUID.fromString(trimmed);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Idempotency-Key must be a valid UUID");
        }
        return trimmed;
    }

    private String calculateFingerprint(String fileName, Long size, String contentType) {
        String payload = fileName.trim().toLowerCase() + "|" + size + "|" + contentType.trim().toLowerCase();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return payload;
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BadRequestException("File name cannot be empty");
        }
        String name = Paths.get(fileName.trim()).getFileName().toString();
        String sanitized = name.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "_").trim();
        if (sanitized.isEmpty()) {
            throw new BadRequestException("File name is invalid");
        }
        return sanitized;
    }

    private void validateFileTypeAndExtension(String fileName, String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new BadRequestException("Content-Type is required");
        }

        String normalizedContentType = normalizeContentType(contentType);
        String lowerCaseName = fileName.toLowerCase();

        String matchingExt = null;
        for (Map.Entry<String, String> entry : ALLOWED_EXTENSIONS_TO_MIME.entrySet()) {
            if (lowerCaseName.endsWith(entry.getKey())) {
                matchingExt = entry.getKey();
                break;
            }
        }

        if (matchingExt == null) {
            throw new BadRequestException("File extension is not allowed. Only PDF, DOC, and DOCX are permitted");
        }

        String expectedMime = ALLOWED_EXTENSIONS_TO_MIME.get(matchingExt);
        if (!expectedMime.equalsIgnoreCase(normalizedContentType)) {
            throw new BadRequestException(
                    "File extension '" + matchingExt + "' does not match Content-Type '" + contentType + "'");
        }
    }

    public static boolean isMatchingContentType(String actual, String declared) {
        if (actual == null || declared == null) {
            return false;
        }
        return normalizeContentType(actual).equalsIgnoreCase(normalizeContentType(declared));
    }

    public static String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int semicolonIdx = contentType.indexOf(';');
        if (semicolonIdx > 0) {
            return contentType.substring(0, semicolonIdx).trim().toLowerCase();
        }
        return contentType.trim().toLowerCase();
    }

    private void validatePresignResponse(String tempKey, String uploadUrl) {
        if (tempKey == null || !tempKey.startsWith("temp/private/job-portal/candidate-cv/")
                || uploadUrl == null || !uploadUrl.startsWith("https://")) {
            throw new StorageException("File service returned an invalid candidate CV upload URL");
        }
    }
}
