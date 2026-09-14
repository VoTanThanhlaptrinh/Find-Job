package com.nlu.applicationProcess.domain.repository;

import com.nlu.applicationProcess.domain.model.ResumeUploadSession;
import com.nlu.applicationProcess.domain.model.ResumeUploadSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeUploadSessionRepository extends JpaRepository<ResumeUploadSession, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ResumeUploadSession s WHERE s.id = :id")
    Optional<ResumeUploadSession> findByIdForUpdate(@Param("id") UUID id);

    Optional<ResumeUploadSession> findByUserIdAndIdempotencyKey(long userId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ResumeUploadSession s WHERE s.userId = :userId AND s.idempotencyKey = :idempotencyKey")
    Optional<ResumeUploadSession> findByUserIdAndIdempotencyKeyForUpdate(@Param("userId") long userId, @Param("idempotencyKey") String idempotencyKey);

    Optional<ResumeUploadSession> findByResumeId(Long resumeId);

    List<ResumeUploadSession> findByStatusAndExpiresAtBefore(
            ResumeUploadSessionStatus status,
            LocalDateTime expiresAt,
            Pageable pageable
    );

    List<ResumeUploadSession> findByStatusAndFinalizingStartedAtBefore(
            ResumeUploadSessionStatus status,
            LocalDateTime threshold,
            Pageable pageable
    );

    List<ResumeUploadSession> findByStatusAndCleanupCompletedAtIsNull(
            ResumeUploadSessionStatus status,
            Pageable pageable
    );
}
