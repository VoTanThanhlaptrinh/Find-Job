package com.job_web.application_process.application;

import com.job_web.application_process.infrastructure.ai.dto.ResumeModel;

public interface AIService {
    ResumeModel processResume(String rawText);
}



