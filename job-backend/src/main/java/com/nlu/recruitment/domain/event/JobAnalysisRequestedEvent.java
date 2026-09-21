package com.nlu.recruitment.domain.event;

import com.nlu.recruitment.api.dto.VectorizeJdRequest;

public record JobAnalysisRequestedEvent(VectorizeJdRequest request) {
}
