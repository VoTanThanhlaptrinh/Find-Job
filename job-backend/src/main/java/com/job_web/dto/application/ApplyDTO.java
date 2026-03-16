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
    @NotNull(message = "Thiáº¿u job id")
    private String jobId;
    @NotNull(message = "Thiáº¿u resume")
    private MultipartFile file;
    @AssertTrue(message = "KÃ­ch thÆ°á»›c file tá»‘i Ä‘a 5mb")
    public boolean isValidFileSize() {
        return file != null && file.getSize() <= 5 * 1024 * 1024;
    }
}






