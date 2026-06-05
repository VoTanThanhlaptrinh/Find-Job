package com.nlu.applicationProcess.application;

import com.nlu.applicationProcess.api.dto.client.ResumeModel;

public interface ResumeParsingService {
    ResumeModel processResume(String rawText);
}



