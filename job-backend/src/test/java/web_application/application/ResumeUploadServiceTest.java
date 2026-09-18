package web_application.application;

import com.nlu.applicationProcess.api.dto.req.ResumeUploadInitiateRequest;
import com.nlu.applicationProcess.api.dto.req.ResumeView;
import com.nlu.applicationProcess.api.dto.res.ResumeUploadInitiateResponse;
import com.nlu.applicationProcess.application.impl.ResumeUploadFinalizer;
import com.nlu.applicationProcess.application.impl.ResumeUploadServiceImpl;
import com.nlu.applicationProcess.domain.model.ClaimResult;
import com.nlu.applicationProcess.domain.model.Resume;
import com.nlu.applicationProcess.domain.model.ResumeStatus;
import com.nlu.applicationProcess.domain.model.ResumeUploadSession;
import com.nlu.applicationProcess.domain.model.ResumeUploadSessionStatus;
import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.applicationProcess.domain.repository.ResumeUploadSessionRepository;
import com.nlu.applicationProcess.infrastructure.redis.ResumeUploadLockService;
import com.nlu.identity.domain.model.User;
import com.nlu.shared.application.CloudStorageService;
import com.nlu.shared.application.FileService;
import com.nlu.shared.application.S3PresignedUrlService;
import com.nlu.shared.domain.exception.*;
import com.nlu.shared.domain.model.PresignedUploadUrlResponse;
import com.nlu.shared.domain.model.StorageObjectMetadata;
import com.nlu.shared.infrastructure.config.ResumeUploadProperties;
import com.nlu.shared.infrastructure.message.MessageProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeUploadServiceTest {

    @Mock
    private ResumeUploadSessionRepository sessionRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private S3PresignedUrlService s3PresignedUrlService;

    @Mock
    private CloudStorageService cloudStorageService;

    @Mock
    private ResumeUploadFinalizer resumeUploadFinalizer;

    @Mock
    private ResumeUploadLockService lockService;

    @Mock
    private ResumeUploadProperties properties;

    // Boundary mocks to verify zero interactions in the new presigned flow
    @Mock
    private FileService fileService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private MessageProducer messageProducer;

    @InjectMocks
    private ResumeUploadServiceImpl resumeUploadService;

    private User testUser;
    private User otherUser;
    private UUID testUploadId;
    private String testIdempotencyKey;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(100L);
        testUser.setEmail(new com.nlu.identity.domain.vo.EmailAddress("user100@test.com"));

        otherUser = new User();
        otherUser.setId(200L);
        otherUser.setEmail(new com.nlu.identity.domain.vo.EmailAddress("user200@test.com"));

        testUploadId = UUID.randomUUID();
        testIdempotencyKey = UUID.randomUUID().toString();

        lenient().when(properties.getMaxFileSizeBytes()).thenReturn(5L * 1024 * 1024);
        lenient().when(properties.getUrlExpirationMinutes()).thenReturn(10);
        lenient().when(properties.getSessionExpirationMinutes()).thenReturn(30);
        lenient().when(properties.getTempPrefix()).thenReturn("temp");
        lenient().when(properties.getPermanentPrefix()).thenReturn("resumes");

        // By default, mock Redis lock acquisition succeeding
        lenient().when(lockService.acquireLock(anyLong(), anyString(), anyString()))
                .thenReturn(Optional.of("token-123"));
        lenient().when(lockService.releaseLock(anyLong(), anyString(), anyString()))
                .thenReturn(true);
    }

    private String calculateFingerprint(String fileName, Long size, String contentType) {
        String payload = fileName.trim().toLowerCase() + "|" + size + "|" + contentType.trim().toLowerCase();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return payload;
        }
    }

    @Nested
    @DisplayName("Initiate Upload Tests (Idempotency & Concurrency)")
    class InitiateUploadTests {

        @Test
        @DisplayName("1. Initiate hợp lệ tạo session PENDING_UPLOAD với idempotencyKey và presigned PUT URL")
        void initiateUpload_Valid_Success() {
            ResumeUploadInitiateRequest request = new ResumeUploadInitiateRequest(
                    "my_cv.pdf",
                    "application/pdf",
                    1024L * 1024L
            );

            when(resumeRepository.countResumesByUser_Id(testUser.getId())).thenReturn(5);
            when(sessionRepository.findByUserIdAndIdempotencyKey(testUser.getId(), testIdempotencyKey))
                    .thenReturn(Optional.empty());

            String expectedUrl = "https://mock-r2.cloudflarestorage.com/temp/upload?sig=abc";
            PresignedUploadUrlResponse mockPresigned = new PresignedUploadUrlResponse(
                    expectedUrl,
                    "PUT",
                    Map.of("Content-Type", "application/pdf"),
                    LocalDateTime.now().plusMinutes(10)
            );

            when(s3PresignedUrlService.generateUploadUrl(anyString(), eq("application/pdf"), any(UUID.class), eq(10)))
                    .thenReturn(mockPresigned);

            ResumeUploadInitiateResponse response = resumeUploadService.initiateUpload(testIdempotencyKey, request, testUser);

            assertNotNull(response);
            assertNotNull(response.uploadId());
            assertEquals(expectedUrl, response.uploadUrl());
            assertEquals("PUT", response.method());
            assertEquals("application/pdf", response.requiredHeaders().get("Content-Type"));

            // Verify session was saved with idempotency key, fingerprint, and correct status
            ArgumentCaptor<ResumeUploadSession> captor = ArgumentCaptor.forClass(ResumeUploadSession.class);
            verify(sessionRepository).save(captor.capture());
            ResumeUploadSession saved = captor.getValue();

            assertEquals(testUser.getId(), saved.getUserId());
            assertEquals(testIdempotencyKey, saved.getIdempotencyKey());
            assertNotNull(saved.getRequestFingerprint());
            assertEquals("my_cv.pdf", saved.getOriginalFileName());
            assertEquals("application/pdf", saved.getDeclaredContentType());
            assertEquals(1024L * 1024L, saved.getDeclaredSize());
            assertEquals(ResumeUploadSessionStatus.PENDING_UPLOAD, saved.getStatus());
            assertNull(saved.getResumeId());

            // Verify Redis lock was released
            verify(lockService).releaseLock(eq(testUser.getId()), eq(testIdempotencyKey), anyString());
        }

        @Test
        @DisplayName("2. Hai request initiate cùng user và idempotency key + cùng payload trả cùng uploadId, không tạo session 2")
        void initiateUpload_Idempotent_SamePayload_ReturnsSameUploadId() {
            ResumeUploadInitiateRequest request = new ResumeUploadInitiateRequest(
                    "my_cv.pdf",
                    "application/pdf",
                    1024L * 1024L
            );

            String fingerprint = calculateFingerprint("my_cv.pdf", 1024L * 1024L, "application/pdf");
            ResumeUploadSession existingSession = ResumeUploadSession.builder()
                    .id(testUploadId)
                    .userId(testUser.getId())
                    .idempotencyKey(testIdempotencyKey)
                    .requestFingerprint(fingerprint)
                    .originalFileName("my_cv.pdf")
                    .declaredContentType("application/pdf")
                    .declaredSize(1024L * 1024L)
                    .tempKey("temp/resumes/100/" + testUploadId)
                    .permanentKey("resumes/100/" + testUploadId)
                    .status(ResumeUploadSessionStatus.PENDING_UPLOAD)
                    .expiresAt(LocalDateTime.now().plusMinutes(20))
                    .build();

            when(sessionRepository.findByUserIdAndIdempotencyKey(testUser.getId(), testIdempotencyKey))
                    .thenReturn(Optional.of(existingSession));

            PresignedUploadUrlResponse refreshedPresigned = new PresignedUploadUrlResponse(
                    "https://mock-r2.cloudflarestorage.com/temp/refreshed",
                    "PUT",
                    Map.of("Content-Type", "application/pdf"),
                    LocalDateTime.now().plusMinutes(10)
            );
            when(s3PresignedUrlService.generateUploadUrl(eq(existingSession.getTempKey()), eq("application/pdf"), eq(testUploadId), eq(10)))
                    .thenReturn(refreshedPresigned);

            ResumeUploadInitiateResponse response = resumeUploadService.initiateUpload(testIdempotencyKey, request, testUser);

            // Must return the SAME uploadId
            assertEquals(testUploadId, response.uploadId());
            assertEquals("https://mock-r2.cloudflarestorage.com/temp/refreshed", response.uploadUrl());

            // Must NOT create a second session in DB
            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("3. Cùng user và idempotency key nhưng payload khác (fingerprint mismatch) trả HTTP 409 Conflict")
        void initiateUpload_Conflict_WhenFingerprintDiffers() {
            ResumeUploadInitiateRequest request = new ResumeUploadInitiateRequest(
                    "new_cv.pdf",
                    "application/pdf",
                    2048L
            );

            // Existing session was created for a 1024-byte file with different fingerprint
            String originalFingerprint = calculateFingerprint("old_cv.pdf", 1024L, "application/pdf");
            ResumeUploadSession existingSession = ResumeUploadSession.builder()
                    .id(testUploadId)
                    .userId(testUser.getId())
                    .idempotencyKey(testIdempotencyKey)
                    .requestFingerprint(originalFingerprint)
                    .originalFileName("old_cv.pdf")
                    .declaredContentType("application/pdf")
                    .declaredSize(1024L)
                    .status(ResumeUploadSessionStatus.PENDING_UPLOAD)
                    .expiresAt(LocalDateTime.now().plusMinutes(20))
                    .build();

            when(sessionRepository.findByUserIdAndIdempotencyKey(testUser.getId(), testIdempotencyKey))
                    .thenReturn(Optional.of(existingSession));

            assertThrows(ConflictException.class, () ->
                    resumeUploadService.initiateUpload(testIdempotencyKey, request, testUser)
            );
        }

        @Test
        @DisplayName("4. Hai user khác nhau được phép dùng cùng chuỗi idempotency key (key được scope theo user)")
        void initiateUpload_TwoUsersCanUseSameKeyString() {
            String sharedKey = UUID.randomUUID().toString();
            ResumeUploadInitiateRequest request = new ResumeUploadInitiateRequest(
                    "cv.pdf",
                    "application/pdf",
                    1024L
            );

            // User 1
            when(sessionRepository.findByUserIdAndIdempotencyKey(testUser.getId(), sharedKey))
                    .thenReturn(Optional.empty());
            // User 2
            when(sessionRepository.findByUserIdAndIdempotencyKey(otherUser.getId(), sharedKey))
                    .thenReturn(Optional.empty());

            PresignedUploadUrlResponse presigned = new PresignedUploadUrlResponse(
                    "https://r2/url", "PUT", Map.of(), LocalDateTime.now().plusMinutes(10));
            when(s3PresignedUrlService.generateUploadUrl(anyString(), anyString(), any(UUID.class), anyInt()))
                    .thenReturn(presigned);

            ResumeUploadInitiateResponse resp1 = resumeUploadService.initiateUpload(sharedKey, request, testUser);
            ResumeUploadInitiateResponse resp2 = resumeUploadService.initiateUpload(sharedKey, request, otherUser);

            assertNotNull(resp1.uploadId());
            assertNotNull(resp2.uploadId());
            assertNotEquals(resp1.uploadId(), resp2.uploadId(), "Each user must receive their own distinct session");
        }

        @Test
        @DisplayName("5. Khi Redis gặp sự cố (outage), DB unique constraint vẫn bảo vệ và request thành công")
        void initiateUpload_RedisOutage_ProtectedByDatabase() {
            ResumeUploadInitiateRequest request = new ResumeUploadInitiateRequest(
                    "cv.pdf",
                    "application/pdf",
                    1024L
            );

            // Simulate Redis outage: acquireLock returns Optional.empty()
            when(lockService.acquireLock(anyLong(), anyString(), anyString())).thenReturn(Optional.empty());
            when(sessionRepository.findByUserIdAndIdempotencyKey(testUser.getId(), testIdempotencyKey))
                    .thenReturn(Optional.empty());

            PresignedUploadUrlResponse presigned = new PresignedUploadUrlResponse(
                    "https://r2/url", "PUT", Map.of(), LocalDateTime.now().plusMinutes(10));
            when(s3PresignedUrlService.generateUploadUrl(anyString(), anyString(), any(UUID.class), anyInt()))
                    .thenReturn(presigned);

            ResumeUploadInitiateResponse response = resumeUploadService.initiateUpload(testIdempotencyKey, request, testUser);

            assertNotNull(response);
            verify(sessionRepository).save(any(ResumeUploadSession.class));
        }

        @Test
        @DisplayName("6. Khi concurrent insert race xảy ra dưới DB (DataIntegrityViolationException), đọc lại session hiện tại và trả về đúng")
        void initiateUpload_ConcurrentRace_DataIntegrityViolation_CatchesAndReturnsExisting() {
            ResumeUploadInitiateRequest request = new ResumeUploadInitiateRequest(
                    "cv.pdf",
                    "application/pdf",
                    1024L
            );

            String fingerprint = calculateFingerprint("cv.pdf", 1024L, "application/pdf");
            ResumeUploadSession racedSession = ResumeUploadSession.builder()
                    .id(testUploadId)
                    .userId(testUser.getId())
                    .idempotencyKey(testIdempotencyKey)
                    .requestFingerprint(fingerprint)
                    .originalFileName("cv.pdf")
                    .declaredContentType("application/pdf")
                    .declaredSize(1024L)
                    .tempKey("temp/resumes/100/" + testUploadId)
                    .status(ResumeUploadSessionStatus.PENDING_UPLOAD)
                    .expiresAt(LocalDateTime.now().plusMinutes(20))
                    .build();

            // First check says empty
            when(sessionRepository.findByUserIdAndIdempotencyKey(testUser.getId(), testIdempotencyKey))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(racedSession)); // Second query after catch returns the raced session

            // save throws DataIntegrityViolationException due to DB unique constraint
            when(sessionRepository.save(any(ResumeUploadSession.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

            PresignedUploadUrlResponse presigned = new PresignedUploadUrlResponse(
                    "https://r2/url", "PUT", Map.of(), LocalDateTime.now().plusMinutes(10));
            when(s3PresignedUrlService.generateUploadUrl(anyString(), anyString(), eq(testUploadId), anyInt()))
                    .thenReturn(presigned);

            ResumeUploadInitiateResponse response = resumeUploadService.initiateUpload(testIdempotencyKey, request, testUser);

            assertNotNull(response);
            assertEquals(testUploadId, response.uploadId());
        }

        @Test
        @DisplayName("7. Từ chối khi thiếu hoặc sai định dạng Idempotency-Key")
        void initiateUpload_RejectInvalidIdempotencyKey() {
            ResumeUploadInitiateRequest request = new ResumeUploadInitiateRequest("cv.pdf", "application/pdf", 1024L);

            // Null or blank
            assertThrows(BadRequestException.class, () ->
                    resumeUploadService.initiateUpload(null, request, testUser)
            );
            assertThrows(BadRequestException.class, () ->
                    resumeUploadService.initiateUpload("   ", request, testUser)
            );

            // Non-UUID format
            assertThrows(BadRequestException.class, () ->
                    resumeUploadService.initiateUpload("not-a-valid-uuid", request, testUser)
            );
        }
    }

    @Nested
    @DisplayName("Complete Upload Tests (State Machine, Idempotency & Invariants)")
    class CompleteUploadTests {

        private ResumeUploadSession createClaimedSession(String token) {
            return ResumeUploadSession.builder()
                    .id(testUploadId)
                    .userId(testUser.getId())
                    .originalFileName("sanitized_cv.pdf")
                    .declaredContentType("application/pdf")
                    .declaredSize(2048L)
                    .tempKey("temp/resumes/100/" + testUploadId)
                    .permanentKey("resumes/100/" + testUploadId)
                    .status(ResumeUploadSessionStatus.FINALIZING)
                    .processingToken(token)
                    .resumeId(null)
                    .expiresAt(LocalDateTime.now().plusMinutes(25))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        @Test
        @DisplayName("1. Complete thành công: claim -> verify -> copy -> verify permanent -> finalize -> delete temp")
        void completeUpload_Success() {
            String token = UUID.randomUUID().toString();
            ResumeUploadSession session = createClaimedSession(token);

            when(resumeUploadFinalizer.claimForFinalizing(testUploadId, testUser))
                    .thenReturn(ClaimResult.claimed(token, session));

            when(cloudStorageService.headObject(session.getPermanentKey())).thenReturn(
                    Optional.empty(),
                    Optional.of(new StorageObjectMetadata(session.getPermanentKey(), 2048L, "application/pdf", "etag-perm"))
            );
            when(cloudStorageService.headObject(session.getTempKey())).thenReturn(
                    Optional.of(new StorageObjectMetadata(session.getTempKey(), 2048L, "application/pdf", "etag-temp"))
            );

            Resume createdResume = new Resume();
            createdResume.setId(555L);
            createdResume.setUser(testUser);
            createdResume.setKeyCf(session.getPermanentKey());
            createdResume.setFileName(session.getOriginalFileName());
            createdResume.setRawText(null);
            createdResume.markUploaded();

            when(resumeUploadFinalizer.finalizeUpload(testUploadId, token, testUser)).thenReturn(createdResume);

            ResumeView result = resumeUploadService.completeUpload(testUploadId, testUser);

            assertNotNull(result);
            assertEquals(555L, result.id());
            assertEquals("sanitized_cv.pdf", result.fileName());
            assertEquals(ResumeStatus.UPLOADED, result.status());

            // Verify server-side copy called
            verify(cloudStorageService).copyObject(session.getTempKey(), session.getPermanentKey());
            // Verify temp deleted best effort
            verify(cloudStorageService).deleteObject(session.getTempKey());
            // Zero interactions with legacy multipart services
            verifyNoInteractions(fileService);
            verifyNoInteractions(eventPublisher);
            verifyNoInteractions(messageProducer);
        }

        @Test
        @DisplayName("2. Idempotency: Complete khi đã COMPLETED trả lại Resume hiện tại, không tạo Resume thứ 2")
        void completeUpload_Idempotent_AlreadyCompleted() {
            when(resumeUploadFinalizer.claimForFinalizing(testUploadId, testUser))
                    .thenReturn(ClaimResult.alreadyCompleted(777L));

            Resume existingResume = new Resume();
            existingResume.setId(777L);
            existingResume.setFileName("cv.pdf");
            existingResume.markUploaded();
            when(resumeRepository.findById(777L)).thenReturn(Optional.of(existingResume));

            ResumeView result = resumeUploadService.completeUpload(testUploadId, testUser);

            assertEquals(777L, result.id());
            // No storage copy, no finalize called again
            verify(cloudStorageService, never()).copyObject(anyString(), anyString());
            verify(resumeUploadFinalizer, never()).finalizeUpload(any(), any(), any());
        }

        @Test
        @DisplayName("3. Request thứ 2 thấy FINALIZING không copy hay reject, chờ kết quả và trả đúng khi hoàn tất")
        void completeUpload_SecondRequestSeesFinalizing_WaitsAndReturnsCompleted() {
            when(resumeUploadFinalizer.claimForFinalizing(testUploadId, testUser))
                    .thenReturn(ClaimResult.inProgress());

            ResumeUploadSession completedSession = createClaimedSession("other-token");
            completedSession.setStatus(ResumeUploadSessionStatus.COMPLETED);
            completedSession.setResumeId(999L);
            when(sessionRepository.findById(testUploadId)).thenReturn(Optional.of(completedSession));

            Resume existingResume = new Resume();
            existingResume.setId(999L);
            existingResume.setFileName("cv.pdf");
            existingResume.markUploaded();
            when(resumeRepository.findById(999L)).thenReturn(Optional.of(existingResume));

            ResumeView result = resumeUploadService.completeUpload(testUploadId, testUser);

            assertEquals(999L, result.id());
            // Zero copy or reject by second request
            verify(cloudStorageService, never()).copyObject(anyString(), anyString());
            verify(resumeUploadFinalizer, never()).markRejected(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("4. Invariant REJECTED: Khi temp object không tồn tại, markRejected được gọi, resumeRepository.save tuyệt đối không gọi")
        void completeUpload_RejectTempNotFound_GuaranteesNoResumeSaved() {
            String token = "tok-1";
            ResumeUploadSession session = createClaimedSession(token);

            when(resumeUploadFinalizer.claimForFinalizing(testUploadId, testUser))
                    .thenReturn(ClaimResult.claimed(token, session));
            when(cloudStorageService.headObject(session.getPermanentKey())).thenReturn(Optional.empty());
            when(cloudStorageService.headObject(session.getTempKey())).thenReturn(Optional.empty());

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    resumeUploadService.completeUpload(testUploadId, testUser)
            );
            assertTrue(ex.getMessage().contains("not found in storage"));

            verify(resumeUploadFinalizer).markRejected(
                    eq(testUploadId), eq(token), eq("TEMP_OBJECT_NOT_FOUND"), anyString(),
                    isNull(), isNull(), isNull()
            );
            verify(resumeRepository, never()).save(any(Resume.class));
            assertNull(session.getResumeId(), "resume_id must remain null");
        }

        @Test
        @DisplayName("5. Invariant REJECTED: Khi size không khớp, markRejected được gọi với size thực tế, resumeRepository.save tuyệt đối không gọi")
        void completeUpload_RejectSizeMismatch_GuaranteesNoResumeSaved() {
            String token = "tok-2";
            ResumeUploadSession session = createClaimedSession(token);

            when(resumeUploadFinalizer.claimForFinalizing(testUploadId, testUser))
                    .thenReturn(ClaimResult.claimed(token, session));
            when(cloudStorageService.headObject(session.getPermanentKey())).thenReturn(Optional.empty());

            StorageObjectMetadata badMeta = new StorageObjectMetadata(
                    session.getTempKey(),
                    9999L, // Declared size is 2048L
                    "application/pdf",
                    "etag123"
            );
            when(cloudStorageService.headObject(session.getTempKey())).thenReturn(Optional.of(badMeta));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    resumeUploadService.completeUpload(testUploadId, testUser)
            );
            assertTrue(ex.getMessage().contains("does not match declared specifications"));

            verify(resumeUploadFinalizer).markRejected(
                    eq(testUploadId), eq(token), eq("METADATA_SIZE_MISMATCH"), anyString(),
                    eq(9999L), eq("application/pdf"), eq("etag123")
            );
            verify(cloudStorageService).deleteObject(session.getTempKey());
            verify(resumeRepository, never()).save(any(Resume.class));
            assertNull(session.getResumeId(), "resume_id must remain null");
        }

        @Test
        @DisplayName("6. Invariant REJECTED: Khi copy xong nhưng verify permanent thất bại, xóa permanent orphan và markRejected")
        void completeUpload_PermanentVerificationFailed_CleansOrphanAndRejects() {
            String token = "tok-3";
            ResumeUploadSession session = createClaimedSession(token);

            when(resumeUploadFinalizer.claimForFinalizing(testUploadId, testUser))
                    .thenReturn(ClaimResult.claimed(token, session));
            when(cloudStorageService.headObject(session.getPermanentKey())).thenReturn(Optional.empty());

            StorageObjectMetadata validTemp = new StorageObjectMetadata(
                    session.getTempKey(), 2048L, "application/pdf", "etag-temp"
            );
            when(cloudStorageService.headObject(session.getTempKey())).thenReturn(Optional.of(validTemp));

            // Copy is called, but verification HEAD returns empty
            StorageException ex = assertThrows(StorageException.class, () ->
                    resumeUploadService.completeUpload(testUploadId, testUser)
            );

            // Must clean up permanent orphan
            verify(cloudStorageService).deleteObject(session.getPermanentKey());
            verify(resumeUploadFinalizer).markRejected(
                    eq(testUploadId), eq(token), eq("PERMANENT_VERIFICATION_FAILED"), anyString(),
                    any(), any(), any()
            );
            verify(resumeRepository, never()).save(any(Resume.class));
            assertNull(session.getResumeId());
        }
    }
}
