package web_application.application;

import com.nlu.applicationProcess.api.dto.req.ResumeUploadInitiateRequest;
import com.nlu.applicationProcess.api.dto.req.ResumeView;
import com.nlu.applicationProcess.api.dto.res.ResumeUploadInitiateResponse;
import com.nlu.applicationProcess.application.impl.ResumeUploadFinalizer;
import com.nlu.applicationProcess.application.impl.ResumeUploadServiceImpl;
import com.nlu.applicationProcess.domain.model.Resume;
import com.nlu.applicationProcess.domain.model.ResumeStatus;
import com.nlu.applicationProcess.domain.model.ResumeUploadSession;
import com.nlu.applicationProcess.domain.model.ResumeUploadSessionStatus;
import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.applicationProcess.domain.repository.ResumeUploadSessionRepository;
import com.nlu.identity.domain.model.User;
import com.nlu.shared.application.CloudStorageService;
import com.nlu.shared.application.FileService;
import com.nlu.shared.application.S3PresignedUrlService;
import com.nlu.shared.domain.exception.BadRequestException;
import com.nlu.shared.domain.exception.ForbiddenException;
import com.nlu.shared.domain.exception.ResourceNotFoundException;
import com.nlu.shared.domain.exception.UnauthorizedException;
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

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(100L);
        testUser.setEmail(new com.nlu.identity.domain.vo.EmailAddress("user100@test.com"));

        otherUser = new User();
        otherUser.setId(200L);
        otherUser.setEmail(new com.nlu.identity.domain.vo.EmailAddress("user200@test.com"));

        testUploadId = UUID.randomUUID();

        lenient().when(properties.getMaxFileSizeBytes()).thenReturn(5L * 1024 * 1024);
        lenient().when(properties.getUrlExpirationMinutes()).thenReturn(10);
        lenient().when(properties.getSessionExpirationMinutes()).thenReturn(30);
        lenient().when(properties.getTempPrefix()).thenReturn("temp");
        lenient().when(properties.getPermanentPrefix()).thenReturn("resumes");
    }

    @Nested
    @DisplayName("1 & 2 & 3: Initiate Upload Tests")
    class InitiateUploadTests {

        @Test
        @DisplayName("1. Initiate hợp lệ tạo session PENDING_UPLOAD và presigned PUT URL")
        void initiateUpload_Valid_Success() {
            ResumeUploadInitiateRequest request = new ResumeUploadInitiateRequest(
                    "my_cv.pdf",
                    "application/pdf",
                    1024L * 1024L
            );

            when(resumeRepository.countResumesByUser_Id(testUser.getId())).thenReturn(5);

            String expectedUrl = "https://mock-r2.cloudflarestorage.com/temp/upload?sig=abc";
            PresignedUploadUrlResponse mockPresigned = new PresignedUploadUrlResponse(
                    expectedUrl,
                    "PUT",
                    Map.of("Content-Type", "application/pdf"),
                    LocalDateTime.now().plusMinutes(10)
            );

            when(s3PresignedUrlService.generateUploadUrl(anyString(), eq("application/pdf"), any(UUID.class), eq(10)))
                    .thenReturn(mockPresigned);

            ResumeUploadInitiateResponse response = resumeUploadService.initiateUpload(request, testUser);

            assertNotNull(response);
            assertNotNull(response.uploadId());
            assertEquals(expectedUrl, response.uploadUrl());
            assertEquals("PUT", response.method());
            assertEquals("application/pdf", response.requiredHeaders().get("Content-Type"));

            // Verify session was saved with correct keys and status
            ArgumentCaptor<ResumeUploadSession> captor = ArgumentCaptor.forClass(ResumeUploadSession.class);
            verify(sessionRepository).save(captor.capture());
            ResumeUploadSession saved = captor.getValue();

            assertEquals(testUser.getId(), saved.getUserId());
            assertEquals("my_cv.pdf", saved.getOriginalFileName());
            assertEquals("application/pdf", saved.getDeclaredContentType());
            assertEquals(1024L * 1024L, saved.getDeclaredSize());
            assertEquals(ResumeUploadSessionStatus.PENDING_UPLOAD, saved.getStatus());
            assertNull(saved.getResumeId());

            // 3. Verify key format: temp/resumes/{userId}/{uploadId} and resumes/{userId}/{uploadId}
            assertTrue(saved.getTempKey().startsWith("temp/resumes/100/"));
            assertTrue(saved.getPermanentKey().startsWith("resumes/100/"));
            assertFalse(saved.getTempKey().contains("my_cv.pdf"), "Object key must not contain original file name");
        }

        @Test
        @DisplayName("2. Từ chối file rỗng (size <= 0)")
        void initiateUpload_RejectEmptyFile() {
            ResumeUploadInitiateRequest request = new ResumeUploadInitiateRequest(
                    "empty.pdf",
                    "application/pdf",
                    0L
            );

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    resumeUploadService.initiateUpload(request, testUser)
            );
            assertTrue(ex.getMessage().contains("greater than 0"));
        }

        @Test
        @DisplayName("2. Từ chối file vượt quá 5 MB")
        void initiateUpload_RejectOversizedFile() {
            ResumeUploadInitiateRequest request = new ResumeUploadInitiateRequest(
                    "large.pdf",
                    "application/pdf",
                    5L * 1024 * 1024 + 1L
            );

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    resumeUploadService.initiateUpload(request, testUser)
            );
            assertTrue(ex.getMessage().contains("5 MB"));
        }

        @Test
        @DisplayName("2. Từ chối extension và MIME không khớp nhau")
        void initiateUpload_RejectExtensionMimeMismatch() {
            // PDF file with Word MIME type
            ResumeUploadInitiateRequest request = new ResumeUploadInitiateRequest(
                    "fake.pdf",
                    "application/msword",
                    1024L
            );

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    resumeUploadService.initiateUpload(request, testUser)
            );
            assertTrue(ex.getMessage().contains("does not match"));
        }

        @Test
        @DisplayName("2. Từ chối định dạng không thuộc PDF, DOC, DOCX")
        void initiateUpload_RejectUnsupportedFormat() {
            ResumeUploadInitiateRequest request = new ResumeUploadInitiateRequest(
                    "malware.exe",
                    "application/octet-stream",
                    1024L
            );

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    resumeUploadService.initiateUpload(request, testUser)
            );
            assertTrue(ex.getMessage().contains("Only PDF, DOC, and DOCX"));
        }

        @Test
        @DisplayName("Initiate yêu cầu người dùng phải đăng nhập")
        void initiateUpload_RejectUnauthenticated() {
            ResumeUploadInitiateRequest request = new ResumeUploadInitiateRequest(
                    "cv.pdf",
                    "application/pdf",
                    1024L
            );

            assertThrows(UnauthorizedException.class, () ->
                    resumeUploadService.initiateUpload(request, null)
            );
        }

        @Test
        @DisplayName("Initiate từ chối khi vượt quá quota 100 CV")
        void initiateUpload_RejectQuotaExceeded() {
            when(resumeRepository.countResumesByUser_Id(testUser.getId())).thenReturn(101);

            ResumeUploadInitiateRequest request = new ResumeUploadInitiateRequest(
                    "cv.pdf",
                    "application/pdf",
                    1024L
            );

            assertThrows(BadRequestException.class, () ->
                    resumeUploadService.initiateUpload(request, testUser)
            );
        }
    }

    @Nested
    @DisplayName("4 - 15: Complete Upload Tests")
    class CompleteUploadTests {

        private ResumeUploadSession createPendingSession() {
            return ResumeUploadSession.builder()
                    .id(testUploadId)
                    .userId(testUser.getId())
                    .originalFileName("sanitized_cv.pdf")
                    .declaredContentType("application/pdf")
                    .declaredSize(2048L)
                    .tempKey("temp/resumes/100/" + testUploadId)
                    .permanentKey("resumes/100/" + testUploadId)
                    .status(ResumeUploadSessionStatus.PENDING_UPLOAD)
                    .resumeId(null)
                    .expiresAt(LocalDateTime.now().plusMinutes(25))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        @Test
        @DisplayName("4. Người dùng không thể complete session của người khác (403)")
        void completeUpload_ForbiddenForOtherUser() {
            ResumeUploadSession session = createPendingSession();
            when(sessionRepository.findById(testUploadId)).thenReturn(Optional.of(session));

            assertThrows(ForbiddenException.class, () ->
                    resumeUploadService.completeUpload(testUploadId, otherUser)
            );
        }

        @Test
        @DisplayName("5. Complete từ chối session hết hạn")
        void completeUpload_RejectExpiredSession() {
            ResumeUploadSession session = createPendingSession();
            session.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // Expired

            when(sessionRepository.findById(testUploadId)).thenReturn(Optional.of(session));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    resumeUploadService.completeUpload(testUploadId, testUser)
            );
            assertTrue(ex.getMessage().contains("expired"));
            assertEquals(ResumeUploadSessionStatus.EXPIRED, session.getStatus());
            verify(sessionRepository).save(session);
        }

        @Test
        @DisplayName("6. Complete từ chối object không tồn tại trên R2 (HEAD temp empty)")
        void completeUpload_RejectTempObjectNotFound() {
            ResumeUploadSession session = createPendingSession();
            when(sessionRepository.findById(testUploadId)).thenReturn(Optional.of(session));
            when(cloudStorageService.headObject(session.getPermanentKey())).thenReturn(Optional.empty());
            when(cloudStorageService.headObject(session.getTempKey())).thenReturn(Optional.empty());

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    resumeUploadService.completeUpload(testUploadId, testUser)
            );
            assertTrue(ex.getMessage().contains("not found in storage"));
            assertEquals(ResumeUploadSessionStatus.REJECTED, session.getStatus());
        }

        @Test
        @DisplayName("7. Complete từ chối khi actual size không khớp declared size")
        void completeUpload_RejectSizeMismatch() {
            ResumeUploadSession session = createPendingSession();
            when(sessionRepository.findById(testUploadId)).thenReturn(Optional.of(session));
            when(cloudStorageService.headObject(session.getPermanentKey())).thenReturn(Optional.empty());

            // Actual size 9999 != declared size 2048
            StorageObjectMetadata badMeta = new StorageObjectMetadata(
                    session.getTempKey(),
                    9999L,
                    "application/pdf",
                    "etag123"
            );
            when(cloudStorageService.headObject(session.getTempKey())).thenReturn(Optional.of(badMeta));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    resumeUploadService.completeUpload(testUploadId, testUser)
            );
            assertTrue(ex.getMessage().contains("does not match"));
            assertEquals(ResumeUploadSessionStatus.REJECTED, session.getStatus());
            verify(cloudStorageService).deleteObject(session.getTempKey());
        }

        @Test
        @DisplayName("7. Complete từ chối khi content type không khớp declared content type")
        void completeUpload_RejectContentTypeMismatch() {
            ResumeUploadSession session = createPendingSession();
            when(sessionRepository.findById(testUploadId)).thenReturn(Optional.of(session));
            when(cloudStorageService.headObject(session.getPermanentKey())).thenReturn(Optional.empty());

            StorageObjectMetadata mismatchTypeMeta = new StorageObjectMetadata(
                    session.getTempKey(),
                    2048L,
                    "image/png",
                    "etag123"
            );
            when(cloudStorageService.headObject(session.getTempKey())).thenReturn(Optional.of(mismatchTypeMeta));

            assertThrows(BadRequestException.class, () ->
                    resumeUploadService.completeUpload(testUploadId, testUser)
            );
            assertEquals(ResumeUploadSessionStatus.REJECTED, session.getStatus());
        }

        @Test
        @DisplayName("8 & 9 & 10 & 11: Complete thành công — server-side copy, UPLOADED status, rawText=null, zero AI/file calls")
        void completeUpload_Success_VerifiesAllContractGuarantees() {
            ResumeUploadSession session = createPendingSession();
            when(sessionRepository.findById(testUploadId)).thenReturn(Optional.of(session));
            when(cloudStorageService.headObject(session.getPermanentKey())).thenReturn(
                    Optional.empty(), // First check: permanent doesn't exist yet
                    Optional.of(new StorageObjectMetadata(session.getPermanentKey(), 2048L, "application/pdf", "etag2")) // Verification check
            );

            StorageObjectMetadata validTempMeta = new StorageObjectMetadata(
                    session.getTempKey(),
                    2048L,
                    "application/pdf",
                    "etag1"
            );
            when(cloudStorageService.headObject(session.getTempKey())).thenReturn(Optional.of(validTempMeta));

            Resume createdResume = new Resume();
            createdResume.setId(555L);
            createdResume.setUser(testUser);
            createdResume.setKeyCf(session.getPermanentKey());
            createdResume.setFileName(session.getOriginalFileName());
            createdResume.setRawText(null);
            createdResume.markUploaded();

            when(resumeUploadFinalizer.finalizeUpload(eq(testUploadId), eq(testUser), eq(session.getPermanentKey()), eq(session.getOriginalFileName())))
                    .thenReturn(createdResume);

            ResumeView result = resumeUploadService.completeUpload(testUploadId, testUser);

            assertNotNull(result);
            assertEquals(555L, result.id());
            assertEquals("sanitized_cv.pdf", result.fileName());
            assertEquals(ResumeStatus.UPLOADED, result.status());

            // 8. Verify server-side copy called (no byte transfer)
            verify(cloudStorageService).copyObject(session.getTempKey(), session.getPermanentKey());

            // Verify temp delete called best-effort
            verify(cloudStorageService).deleteObject(session.getTempKey());

            // 9. Verify created resume status and null rawText
            assertEquals(ResumeStatus.UPLOADED, createdResume.getStatus());
            assertNull(createdResume.getRawText(), "rawText must be null in presigned flow");

            // 10. Verify FileService was NEVER called
            verifyNoInteractions(fileService);

            // 11. Verify ApplicationEventPublisher and MessageProducer were NEVER called
            verifyNoInteractions(eventPublisher);
            verifyNoInteractions(messageProducer);
        }

        @Test
        @DisplayName("12. Idempotency: Gọi complete hai lần trả cùng ResumeView")
        void completeUpload_Idempotent_SecondCallReturnsExistingResume() {
            ResumeUploadSession completedSession = createPendingSession();
            completedSession.setStatus(ResumeUploadSessionStatus.COMPLETED);
            completedSession.setResumeId(777L);

            when(sessionRepository.findById(testUploadId)).thenReturn(Optional.of(completedSession));

            Resume existingResume = new Resume();
            existingResume.setId(777L);
            existingResume.setFileName("my_cv.pdf");
            existingResume.markUploaded();
            when(resumeRepository.findById(777L)).thenReturn(Optional.of(existingResume));

            ResumeView result = resumeUploadService.completeUpload(testUploadId, testUser);

            assertEquals(777L, result.id());
            assertEquals("my_cv.pdf", result.fileName());

            // Verify no storage copy or finalizer was executed again
            verify(cloudStorageService, never()).copyObject(anyString(), anyString());
            verify(resumeUploadFinalizer, never()).finalizeUpload(any(), any(), any(), any());
        }

        @Test
        @DisplayName("14. Xóa temp thất bại không làm complete thất bại")
        void completeUpload_TempDeleteFailure_DoesNotFailComplete() {
            ResumeUploadSession session = createPendingSession();
            when(sessionRepository.findById(testUploadId)).thenReturn(Optional.of(session));
            when(cloudStorageService.headObject(session.getPermanentKey())).thenReturn(
                    Optional.empty(),
                    Optional.of(new StorageObjectMetadata(session.getPermanentKey(), 2048L, "application/pdf", "etag2"))
            );
            when(cloudStorageService.headObject(session.getTempKey())).thenReturn(
                    Optional.of(new StorageObjectMetadata(session.getTempKey(), 2048L, "application/pdf", "etag1"))
            );

            Resume createdResume = new Resume();
            createdResume.setId(888L);
            createdResume.markUploaded();
            when(resumeUploadFinalizer.finalizeUpload(any(), any(), any(), any())).thenReturn(createdResume);

            // Mock temp deletion throwing exception
            doThrow(new RuntimeException("Cloudflare R2 temporary network glitch"))
                    .when(cloudStorageService).deleteObject(session.getTempKey());

            // Complete must still succeed!
            ResumeView result = assertDoesNotThrow(() ->
                    resumeUploadService.completeUpload(testUploadId, testUser)
            );
            assertEquals(888L, result.id());
        }

        @Test
        @DisplayName("Permanent object đã tồn tại từ lần thử trước thì xác minh metadata và sử dụng lại (không copy lại)")
        void completeUpload_PermanentObjectAlreadyExists_SkipsCopy() {
            ResumeUploadSession session = createPendingSession();
            when(sessionRepository.findById(testUploadId)).thenReturn(Optional.of(session));

            // Permanent object already exists with matching declared metadata
            StorageObjectMetadata existingPermMeta = new StorageObjectMetadata(
                    session.getPermanentKey(),
                    2048L,
                    "application/pdf",
                    "etag-perm"
            );
            when(cloudStorageService.headObject(session.getPermanentKey())).thenReturn(Optional.of(existingPermMeta));

            Resume createdResume = new Resume();
            createdResume.setId(999L);
            createdResume.markUploaded();
            when(resumeUploadFinalizer.finalizeUpload(any(), any(), any(), any())).thenReturn(createdResume);

            ResumeView result = resumeUploadService.completeUpload(testUploadId, testUser);

            assertEquals(999L, result.id());
            // Copy was NOT called again
            verify(cloudStorageService, never()).copyObject(anyString(), anyString());
            // Temp delete is still performed
            verify(cloudStorageService).deleteObject(session.getTempKey());
        }
    }
}
