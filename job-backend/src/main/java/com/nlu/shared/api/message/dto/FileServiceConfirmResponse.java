package com.nlu.shared.api.message.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FileServiceConfirmResponse(
        String key,
        String visibility,
        String url
) {
}
