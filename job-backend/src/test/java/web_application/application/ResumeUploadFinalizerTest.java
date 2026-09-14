package web_application.application;

import com.nlu.applicationProcess.application.impl.ResumeUploadFinalizer;
import com.nlu.applicationProcess.domain.model.ClaimResult;
import com.nlu.applicationProcess.domain.model.Resume;
import com.nlu.applicationProcess.domain.model.ResumeStatus;
import com.nlu.applicationProcess.domain.model.ResumeUploadSession;
import com.nlu.applicationProcess.domain.model.ResumeUploadSessionStatus;
import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.applicationProcess.domain.repository.ResumeUploadSessionRepository;
import com.nlu.identity.domain.model.User;
import com.nlu.shared.domain.exception.BadRequestException;
import com.nlu.shared.domain.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeUploadFinalizerTest {

    @Mock
    private ResumeUploadSessionRepository sessionRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @InjectMocks
    private ResumeUploadFinalizer resumeUploadFinalizer;

    private User user;
    private User otherUser;
    private UUID uploadId;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(42L);
        user.setEmail(new com.nlu.identity.domain.vo.EmailAddress("user42@test.com"));

        otherUser = new User();
        otherUser.setId(99L);
        otherUser.setEmail(new com.nlu.identity.domain.vo.EmailAddress("user99@test.com"));

        uploadId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("claimForFinalizing Tests")
    class ClaimForFinalizingTests {

        @Test
        @DisplayName("Claim thành công chuyển PENDING_UPLOAD sang FINALIZING với processingToken")
        void claimForFinalizing_Success() {
            ResumeUploadSession session = ResumeUploadSession.builder()
                    .id(uploadId)
                    .userId(42L)
                    .status(ResumeUploadSessionStatus.PENDING_UPLOAD)
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build();

            when(sessionRepository.findByIdForUpdate(uploadId)).thenReturn(Optional.of(session));

            ClaimResult result = resumeUploadFinalizer.claimForFinalizing(uploadId, user);

            assertEquals(ClaimResult.ClaimStatus.CLAIMED, result.status());
            assertNotNull(result.processingToken());
            assertEquals(ResumeUploadSessionStatus.FINALIZING, session.getStatus());
            assertEquals(result.processingToken(), session.getProcessingToken());
            assertNotNull(session.getFinalizingStartedAt());
            verify(sessionRepository).save(session);
        }

        @Test
        @DisplayName("Claim từ chối khi session thuộc user khác (403 Forbidden)")
        void claimForFinalizing_ForbiddenForOtherUser() {
            ResumeUploadSession session = ResumeUploadSession.builder()
                    .id(uploadId)
                    .userId(42L)
                    .status(ResumeUploadSessionStatus.PENDING_UPLOAD)
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build();

            when(sessionRepository.findByIdForUpdate(uploadId)).thenReturn(Optional.of(session));

            assertThrows(ForbiddenException.class, () ->
                    resumeUploadFinalizer.claimForFinalizing(uploadId, otherUser)
            );
        }

        @Test
        @DisplayName("Claim trả về ALREADY_COMPLETED khi session đã hoàn tất")
        void claimForFinalizing_WhenAlreadyCompleted() {
            ResumeUploadSession session = ResumeUploadSession.builder()
                    .id(uploadId)
                    .userId(42L)
                    .status(ResumeUploadSessionStatus.COMPLETED)
                    .resumeId(888L)
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build();

            when(sessionRepository.findByIdForUpdate(uploadId)).thenReturn(Optional.of(session));

            ClaimResult result = resumeUploadFinalizer.claimForFinalizing(uploadId, user);

            assertEquals(ClaimResult.ClaimStatus.ALREADY_COMPLETED, result.status());
            assertEquals(888L, result.existingResumeId());
            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Claim ném BadRequestException riêng khi session EXPIRED")
        void claimForFinalizing_RejectsExpired() {
            ResumeUploadSession session = ResumeUploadSession.builder()
                    .id(uploadId)
                    .userId(42L)
                    .status(ResumeUploadSessionStatus.EXPIRED)
                    .expiresAt(LocalDateTime.now().minusMinutes(5))
                    .build();

            when(sessionRepository.findByIdForUpdate(uploadId)).thenReturn(Optional.of(session));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    resumeUploadFinalizer.claimForFinalizing(uploadId, user)
            );
            assertTrue(ex.getMessage().contains("expired"));
        }

        @Test
        @DisplayName("Claim ném BadRequestException riêng khi session REJECTED")
        void claimForFinalizing_RejectsRejected() {
            ResumeUploadSession session = ResumeUploadSession.builder()
                    .id(uploadId)
                    .userId(42L)
                    .status(ResumeUploadSessionStatus.REJECTED)
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build();

            when(sessionRepository.findByIdForUpdate(uploadId)).thenReturn(Optional.of(session));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    resumeUploadFinalizer.claimForFinalizing(uploadId, user)
            );
            assertTrue(ex.getMessage().contains("rejected"));
        }

        @Test
        @DisplayName("Claim trả về IN_PROGRESS khi một worker khác đang FINALIZING trong lease")
        void claimForFinalizing_ReturnsInProgressWhenLeaseActive() {
            ResumeUploadSession session = ResumeUploadSession.builder()
                    .id(uploadId)
                    .userId(42L)
                    .status(ResumeUploadSessionStatus.FINALIZING)
                    .processingToken("active-token")
                    .finalizingStartedAt(LocalDateTime.now().minusSeconds(30))
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build();

            when(sessionRepository.findByIdForUpdate(uploadId)).thenReturn(Optional.of(session));

            ClaimResult result = resumeUploadFinalizer.claimForFinalizing(uploadId, user);

            assertEquals(ClaimResult.ClaimStatus.IN_PROGRESS, result.status());
            verify(sessionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("finalizeUpload Tests")
    class FinalizeUploadTests {

        @Test
        @DisplayName("Finalizer tạo đúng một Resume với status UPLOADED và rawText null, cập nhật session COMPLETED")
        void finalizeUpload_Success() {
            String token = UUID.randomUUID().toString();
            ResumeUploadSession session = ResumeUploadSession.builder()
                    .id(uploadId)
                    .userId(42L)
                    .originalFileName("my_resume.pdf")
                    .declaredContentType("application/pdf")
                    .declaredSize(1024L)
                    .tempKey("temp/resumes/42/" + uploadId)
                    .permanentKey("resumes/42/" + uploadId)
                    .status(ResumeUploadSessionStatus.FINALIZING)
                    .processingToken(token)
                    .build();

            when(sessionRepository.findByIdForUpdate(uploadId)).thenReturn(Optional.of(session));
            when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
                Resume r = invocation.getArgument(0);
                r.setId(1001L);
                return r;
            });

            Resume result = resumeUploadFinalizer.finalizeUpload(uploadId, token, user);

            assertNotNull(result);
            assertEquals(1001L, result.getId());
            assertEquals(ResumeStatus.UPLOADED, result.getStatus());
            assertNull(result.getRawText(), "rawText must be null");
            assertEquals("resumes/42/" + uploadId, result.getKeyCf());
            assertEquals("my_resume.pdf", result.getFileName());

            // Session must be updated to COMPLETED with resumeId
            assertEquals(ResumeUploadSessionStatus.COMPLETED, session.getStatus());
            assertEquals(1001L, session.getResumeId());
            assertNull(session.getProcessingToken());
            verify(sessionRepository).save(session);
        }

        @Test
        @DisplayName("Finalizer idempotent: Khi session đã COMPLETED dưới lock, trả về Resume hiện tại, không tạo Resume mới")
        void finalizeUpload_Idempotent_WhenAlreadyCompleted() {
            ResumeUploadSession session = ResumeUploadSession.builder()
                    .id(uploadId)
                    .userId(42L)
                    .originalFileName("my_resume.pdf")
                    .status(ResumeUploadSessionStatus.COMPLETED)
                    .resumeId(1001L)
                    .build();

            Resume existingResume = new Resume();
            existingResume.setId(1001L);
            existingResume.markUploaded();

            when(sessionRepository.findByIdForUpdate(uploadId)).thenReturn(Optional.of(session));
            when(resumeRepository.findById(1001L)).thenReturn(Optional.of(existingResume));

            Resume result = resumeUploadFinalizer.finalizeUpload(uploadId, "some-token", user);

            assertEquals(1001L, result.getId());
            verify(resumeRepository, never()).save(any(Resume.class));
        }

        @Test
        @DisplayName("Finalizer từ chối session EXPIRED riêng biệt")
        void finalizeUpload_RejectsExpiredSeparately() {
            ResumeUploadSession session = ResumeUploadSession.builder()
                    .id(uploadId)
                    .userId(42L)
                    .status(ResumeUploadSessionStatus.EXPIRED)
                    .build();

            when(sessionRepository.findByIdForUpdate(uploadId)).thenReturn(Optional.of(session));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    resumeUploadFinalizer.finalizeUpload(uploadId, "token", user)
            );
            assertTrue(ex.getMessage().contains("expired"));
            verify(resumeRepository, never()).save(any(Resume.class));
        }

        @Test
        @DisplayName("Finalizer từ chối session REJECTED riêng biệt")
        void finalizeUpload_RejectsRejectedSeparately() {
            ResumeUploadSession session = ResumeUploadSession.builder()
                    .id(uploadId)
                    .userId(42L)
                    .status(ResumeUploadSessionStatus.REJECTED)
                    .build();

            when(sessionRepository.findByIdForUpdate(uploadId)).thenReturn(Optional.of(session));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    resumeUploadFinalizer.finalizeUpload(uploadId, "token", user)
            );
            assertTrue(ex.getMessage().contains("rejected"));
            verify(resumeRepository, never()).save(any(Resume.class));
        }

        @Test
        @DisplayName("Finalizer từ chối khi processing token không khớp")
        void finalizeUpload_RejectsMismatchedToken() {
            ResumeUploadSession session = ResumeUploadSession.builder()
                    .id(uploadId)
                    .userId(42L)
                    .status(ResumeUploadSessionStatus.FINALIZING)
                    .processingToken("valid-token")
                    .build();

            when(sessionRepository.findByIdForUpdate(uploadId)).thenReturn(Optional.of(session));

            assertThrows(BadRequestException.class, () ->
                    resumeUploadFinalizer.finalizeUpload(uploadId, "wrong-token", user)
            );
            verify(resumeRepository, never()).save(any(Resume.class));
        }
    }

    @Nested
    @DisplayName("markRejected Tests")
    class MarkRejectedTests {

        @Test
        @DisplayName("markRejected chuyển session thành REJECTED, resume_id luôn null, tuyệt đối không save Resume")
        void markRejected_Success_GuaranteesNullResumeIdAndZeroResumeSaves() {
            String token = "my-token";
            ResumeUploadSession session = ResumeUploadSession.builder()
                    .id(uploadId)
                    .userId(42L)
                    .status(ResumeUploadSessionStatus.FINALIZING)
                    .processingToken(token)
                    .resumeId(null)
                    .build();

            when(sessionRepository.findByIdForUpdate(uploadId)).thenReturn(Optional.of(session));

            resumeUploadFinalizer.markRejected(
                    uploadId,
                    token,
                    "METADATA_SIZE_MISMATCH",
                    "Uploaded file size does not match declared size",
                    9999L,
                    "application/pdf",
                    "etag123"
            );

            assertEquals(ResumeUploadSessionStatus.REJECTED, session.getStatus());
            assertNull(session.getResumeId(), "resume_id must remain null when rejected");
            assertEquals("METADATA_SIZE_MISMATCH", session.getRejectionCode());
            assertEquals("Uploaded file size does not match declared size", session.getRejectionDetail());
            assertEquals(9999L, session.getActualSize());
            assertEquals("application/pdf", session.getActualContentType());
            assertEquals("etag123", session.getActualEtag());
            assertNotNull(session.getRejectedAt());
            assertNull(session.getProcessingToken());

            verify(resumeRepository, never()).save(any(Resume.class));
            verify(sessionRepository).save(session);
        }

        @Test
        @DisplayName("markRejected không bao giờ ghi đè một session đã COMPLETED")
        void markRejected_NeverOverwritesCompletedSession() {
            ResumeUploadSession session = ResumeUploadSession.builder()
                    .id(uploadId)
                    .userId(42L)
                    .status(ResumeUploadSessionStatus.COMPLETED)
                    .resumeId(1234L)
                    .build();

            when(sessionRepository.findByIdForUpdate(uploadId)).thenReturn(Optional.of(session));

            resumeUploadFinalizer.markRejected(
                    uploadId,
                    "token",
                    "LATE_ERROR",
                    "detail",
                    null, null, null
            );

            // Must stay COMPLETED with resumeId 1234
            assertEquals(ResumeUploadSessionStatus.COMPLETED, session.getStatus());
            assertEquals(1234L, session.getResumeId());
            verify(sessionRepository, never()).save(session);
            verify(resumeRepository, never()).save(any(Resume.class));
        }
    }
}
