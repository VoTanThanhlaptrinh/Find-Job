package com.nlu.applicationProcess.domain.event;

import com.nlu.applicationProcess.api.dto.client.ResumeParsingMessage;

public record ResumeAnalysisRequestedEvent(ResumeParsingMessage message) {
}
