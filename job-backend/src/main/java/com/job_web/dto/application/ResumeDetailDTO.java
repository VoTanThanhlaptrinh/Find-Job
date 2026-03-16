package com.job_web.dto.application;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ResumeDetailDTO {
    private long id;
    private String fileName;
    private LocalDateTime createDate;
}
