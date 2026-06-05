package com.nlu.applicationProcess.api.dto.client;

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
