package com.job_web.dto.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplyCvWithUploadRequest {
    @NotBlank(message = "Job ID is required.")
    private Long jobId;

    @NotNull(message = "CV file is required.")
    private MultipartFile cvFile;

    @NotBlank(message = "Email is required.")
    @Email(message = "Email is invalid.")
    private String email;

    private String coverLetter;
}
