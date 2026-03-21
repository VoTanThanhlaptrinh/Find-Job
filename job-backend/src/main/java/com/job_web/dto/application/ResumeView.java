package com.job_web.dto.application;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ResumeView {
    private long id;
    private String fitleName;
    private LocalDateTime createDate;
}
