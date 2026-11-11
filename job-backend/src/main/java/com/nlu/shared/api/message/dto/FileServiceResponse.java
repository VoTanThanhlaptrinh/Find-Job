package com.nlu.shared.api.message.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FileServiceResponse<T>(
        boolean success,
        T data,
        String message
) {
}
