package com.job_web.dto.application;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record ResumeUploadDTO(
        @NotNull(message = "Resume file must not be empty.")
        MultipartFile file
) {
    @AssertTrue(message = "Only PDF, DOC, or DOCX files are allowed.")
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

    @AssertTrue(message = "Maximum resume size is 5 MB.")
    public boolean isValidSize() {
        return file == null || file.getSize() <= 5 * 1024 * 1024;
    }

    public MultipartFile getFile() {
        return file;
    }
}
