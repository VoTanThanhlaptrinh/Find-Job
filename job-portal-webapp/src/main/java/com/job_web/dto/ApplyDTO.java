package com.job_web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplyDTO {
    @NotNull(message = "Thiếu job id")
    private String jobId;
    @NotNull(message = "Thiếu cv")
    private MultipartFile file;
    @AssertTrue(message = "Kích thước file tối đa 5mb")
    public boolean isValidFileSize() {
        return file != null && file.getSize() <= 5 * 1024 * 1024;
    }
}
