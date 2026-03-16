package com.job_web.dto.ai;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class VerificationResult {
    private boolean isValid;
    private int confidenceScore; // Điểm từ 0 đến 100
    private List<String> errors;
    private String suggestion;
}
