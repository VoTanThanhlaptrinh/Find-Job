package com.job_web.service.ai;

import com.job_web.dto.ai.ResumeModel;

public interface AIService {
    ResumeModel processResume(String rawText);
}



