package com.nlu.shared.api.message.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FileServicePresignResponse(
        String originalName,
        String tempKey,
        String uploadUrl,
        int expiresIn
) {
}
