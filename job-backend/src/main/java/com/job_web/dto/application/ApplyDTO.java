package com.job_web.dto.application;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplyDTO {
    @NotNull(message = "Job ID is required.")
    private String jobId;
    @NotNull(message = "Resume file is required.")
    private MultipartFile file;
    @AssertTrue(message = "Maximum file size is 5 MB.")
    public boolean isValidFileSize() {
        return file != null && file.getSize() <= 5 * 1024 * 1024;
    }
}