package com.job_web.dto.ai;

import java.util.List;

public record VerificationResult(
        boolean isValid,
        int confidenceScore,
        List<String> errors,
        String suggestion
) {
    public int getConfidenceScore() {
        return confidenceScore;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getSuggestion() {
        return suggestion;
    }
}
