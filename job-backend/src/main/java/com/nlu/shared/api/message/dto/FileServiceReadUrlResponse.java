package com.nlu.shared.api.message.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FileServiceReadUrlResponse(
        String key,
        String readUrl,
        int expiresIn
) {
}
