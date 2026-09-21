package web_application.application;

import com.nlu.applicationProcess.application.impl.ResumeUploadFinalizer;
import com.nlu.applicationProcess.domain.model.ClaimResult;
import com.nlu.applicationProcess.domain.model.ResumeUploadSession;
import com.nlu.applicationProcess.domain.model.ResumeUploadSessionStatus;
import com.nlu.applicationProcess.domain.repository.ResumeUploadSessionRepository;
import com.nlu.identity.domain.model.User;
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
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = JobPortalWebApplication.class)
class ResumeUploadDatabaseIntegrationTest {

    static {
        Dotenv dotenv = null;
        try {
            dotenv = Dotenv.configure().directory(".").ignoreIfMissing().load();
        } catch (Exception ignored) {
        }
        if (dotenv == null || dotenv.entries().isEmpty()) {
            try {
                dotenv = Dotenv.configure().directory("d:/web-project/job-backend").ignoreIfMissing().load();
            } catch (Exception ignored) {
            }
        }
        if (dotenv != null) {
            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
            });
        }
    }

    @Autowired
    private ResumeUploadSessionRepository sessionRepository;

    @Autowired
    private ResumeUploadFinalizer resumeUploadFinalizer;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private static boolean schemaInitialized = false;

    @org.junit.jupiter.api.BeforeEach
    void initSchemaOnce() {
        if (!schemaInitialized) {
            try {
                jdbcTemplate.execute(
                        "ALTER TABLE resume_upload_session DROP CONSTRAINT IF EXISTS resume_upload_session_status_check;");
                jdbcTemplate.execute(
                        "ALTER TABLE resume_upload_session ADD CONSTRAINT resume_upload_session_status_check " +
                                "CHECK (status IN ('PENDING_UPLOAD', 'FINALIZING', 'COMPLETED', 'REJECTED', 'EXPIRED'));");

                jdbcTemplate.execute(
                        "ALTER TABLE resume_upload_session DROP CONSTRAINT IF EXISTS chk_resume_upload_session_status_resume_id;");
                jdbcTemplate.execute(
                        "ALTER TABLE resume_upload_session ADD CONSTRAINT chk_resume_upload_session_status_resume_id " +
                                "CHECK ((status = 'COMPLETED' AND resume_id IS NOT NULL) OR (status <> 'COMPLETED' AND resume_id IS NULL));");
            } catch (Exception e) {
                System.err.println("Warning initializing DB constraints in test: " + e.getMessage());
            }
            schemaInitialized = true;
        }
    }

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
    @DisplayName("DB Invariant: REJECTED session với resume_id null được lưu thành công")
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

    @Test
    @DisplayName("DB Check Constraint: Lưu REJECTED session với resume_id != null bị DataIntegrityViolationException")
    void testInvariant_RejectedSession_WithNonNullResumeId_ThrowsDataIntegrityViolation() {
        long userId = 999995L;
        String key = UUID.randomUUID().toString();

        ResumeUploadSession session = buildSession(userId, key);
        session.setStatus(ResumeUploadSessionStatus.REJECTED);
        session.setResumeId(12345L); // Violates check constraint
        session.setRejectionCode("METADATA_SIZE_MISMATCH");
        session.setRejectionDetail("Uploaded file size does not match");
        session.setRejectedAt(LocalDateTime.now());

        assertThrows(DataIntegrityViolationException.class, () -> {
            sessionRepository.saveAndFlush(session);
        });
    }

    @Test
    @DisplayName("DB Check Constraint: Lưu COMPLETED session với resume_id == null bị DataIntegrityViolationException")
    void testInvariant_CompletedSession_WithNullResumeId_ThrowsDataIntegrityViolation() {
        long userId = 999996L;
        String key = UUID.randomUUID().toString();

        ResumeUploadSession session = buildSession(userId, key);
        session.setStatus(ResumeUploadSessionStatus.COMPLETED);
        session.setResumeId(null); // Violates check constraint

        assertThrows(DataIntegrityViolationException.class, () -> {
            sessionRepository.saveAndFlush(session);
        });
    }

    @Test
    @DisplayName("Hai luồng gọi claimForFinalizing đồng thời bằng DB thật: Chỉ duy nhất 1 luồng CLAIMED, luồng kia nhận IN_PROGRESS")
    void testConcurrentClaimForFinalizing_OnlyOneWorkerSucceeds() throws Exception {
        long userId = 999997L;
        String key = UUID.randomUUID().toString();

        ResumeUploadSession session = buildSession(userId, key);
        sessionRepository.saveAndFlush(session);

        User testUser = new User();
        testUser.setId(userId);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<ClaimResult>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                return resumeUploadFinalizer.claimForFinalizing(session.getId(), testUser);
            }));
        }

        readyLatch.await();
        startLatch.countDown(); // Release both threads simultaneously

        List<ClaimResult> results = new ArrayList<>();
        for (Future<ClaimResult> future : futures) {
            results.add(future.get(10, TimeUnit.SECONDS));
        }
        executor.shutdown();

        long claimedCount = results.stream().filter(r -> r.status() == ClaimResult.ClaimStatus.CLAIMED).count();
        long inProgressCount = results.stream().filter(r -> r.status() == ClaimResult.ClaimStatus.IN_PROGRESS).count();

        assertEquals(1, claimedCount, "Chỉ duy nhất 1 luồng được CLAIMED");
        assertEquals(1, inProgressCount, "Luồng còn lại phải nhận IN_PROGRESS");
    }
}
