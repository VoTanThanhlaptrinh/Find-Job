package com.job_web.dto.application;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ResumeUploadDTO {
    @NotNull(message = "Resume khÃ´ng Ä‘Æ°á»£c rá»—ng")
    private MultipartFile file;

    @AssertTrue(message = "Chá»‰ cháº¥p nháº­n PDF, DOC hoáº·c DOCX")
    public boolean isValidType() {
        if (file == null) {
            return true;
        }
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        boolean byContentType = contentType != null && (
                contentType.equals("application/pdf")
                        || contentType.equals("application/msword")
                        || contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        );
        boolean byExtension = fileName != null && (
                fileName.toLowerCase().endsWith(".pdf")
                        || fileName.toLowerCase().endsWith(".doc")
                        || fileName.toLowerCase().endsWith(".docx")
        );
        return byContentType || byExtension;
    }

    @AssertTrue(message = "Dung lÆ°á»£ng Resume tá»‘i Ä‘a 5MB")
    public boolean isValidSize() {
        return file == null || file.getSize() <= 5 * 1024 * 1024;
    }
}

