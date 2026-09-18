package com.nlu.applicationProcess.domain.model;

public record ClaimResult(
        ClaimStatus status,
        String processingToken,
        Long existingResumeId,
        ResumeUploadSession session
) {
    public enum ClaimStatus {
        CLAIMED,
        ALREADY_COMPLETED,
        IN_PROGRESS
    }

    public static ClaimResult claimed(String token, ResumeUploadSession session) {
        return new ClaimResult(ClaimStatus.CLAIMED, token, null, session);
    }

    public static ClaimResult alreadyCompleted(Long resumeId) {
        return new ClaimResult(ClaimStatus.ALREADY_COMPLETED, null, resumeId, null);
    }

    public static ClaimResult inProgress() {
        return new ClaimResult(ClaimStatus.IN_PROGRESS, null, null, null);
    }
}
