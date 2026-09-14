package web_application.application;

import com.nlu.applicationProcess.domain.model.ResumeUploadSession;
import com.nlu.applicationProcess.domain.model.ResumeUploadSessionStatus;
import com.nlu.applicationProcess.domain.repository.ResumeUploadSessionRepository;
import com.nlu.JobPortalWebApplication;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = JobPortalWebApplication.class)
class ResumeUploadDatabaseIntegrationTest {

    static {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
    }

    @Autowired
    private ResumeUploadSessionRepository sessionRepository;

    private final List<UUID> createdSessionIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        for (UUID id : createdSessionIds) {
            try {
                sessionRepository.deleteById(id);
            } catch (Exception ignored) {
            }
        }
    }

    private ResumeUploadSession buildSession(long userId, String idempotencyKey) {
        UUID id = UUID.randomUUID();
        createdSessionIds.add(id);
        LocalDateTime now = LocalDateTime.now();
        return ResumeUploadSession.builder()
                .id(id)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .requestFingerprint("fingerprint-" + id)
                .originalFileName("test.pdf")
                .declaredContentType("application/pdf")
                .declaredSize(1024L)
                .tempKey("temp/resumes/" + userId + "/" + id)
                .permanentKey("resumes/" + userId + "/" + id)
                .status(ResumeUploadSessionStatus.PENDING_UPLOAD)
                .expiresAt(now.plusMinutes(30))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    @DisplayName("DB Unique Constraint: Cùng user và cùng idempotency_key bị DataIntegrityViolationException")
    void testUniqueConstraint_SameUserSameKey_ThrowsDataIntegrityViolation() {
        long userId = 999991L;
        String key = UUID.randomUUID().toString();

        ResumeUploadSession session1 = buildSession(userId, key);
        sessionRepository.saveAndFlush(session1);

        ResumeUploadSession session2 = buildSession(userId, key);

        assertThrows(DataIntegrityViolationException.class, () -> {
            sessionRepository.saveAndFlush(session2);
        });
    }

    @Test
    @DisplayName("DB Scope theo User: Hai user khác nhau có thể sử dụng cùng chuỗi idempotency key thành công")
    void testUniqueConstraint_DifferentUsersSameKey_Success() {
        long user1 = 999992L;
        long user2 = 999993L;
        String sharedKey = UUID.randomUUID().toString();

        ResumeUploadSession session1 = buildSession(user1, sharedKey);
        ResumeUploadSession session2 = buildSession(user2, sharedKey);

        assertDoesNotThrow(() -> {
            sessionRepository.saveAndFlush(session1);
            sessionRepository.saveAndFlush(session2);
        });

        assertNotNull(sessionRepository.findById(session1.getId()).orElse(null));
        assertNotNull(sessionRepository.findById(session2.getId()).orElse(null));
    }

    @Test
    @DisplayName("DB Invariant: REJECTED session phải có resume_id null")
    void testInvariant_RejectedSession_MustHaveNullResumeId() {
        long userId = 999994L;
        String key = UUID.randomUUID().toString();

        ResumeUploadSession session = buildSession(userId, key);
        session.setStatus(ResumeUploadSessionStatus.REJECTED);
        session.setResumeId(null);
        session.setRejectionCode("METADATA_SIZE_MISMATCH");
        session.setRejectionDetail("Uploaded file size does not match");
        session.setRejectedAt(LocalDateTime.now());

        ResumeUploadSession saved = sessionRepository.saveAndFlush(session);
        assertNotNull(saved);
        assertNull(saved.getResumeId(), "resume_id must be null for REJECTED session");
        assertEquals(ResumeUploadSessionStatus.REJECTED, saved.getStatus());
    }
}
