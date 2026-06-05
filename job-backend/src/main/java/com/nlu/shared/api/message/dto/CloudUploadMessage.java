package com.nlu.shared.api.message.dto;

public record CloudUploadMessage(
    byte[] data,
    String key,
    String originalName
) {
}
