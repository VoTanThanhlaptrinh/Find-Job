package com.job_web.dto.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCvWithExistingRequest {
    @NotBlank(message = "Job ID is required.")
    private Long jobId;

    @NotBlank(message = "Existing CV ID is required.")
    private Long existingCvId;

    @NotBlank(message = "Email is required.")
    @Email(message = "Email is invalid.")
    private String email;

    private String coverLetter;
}
