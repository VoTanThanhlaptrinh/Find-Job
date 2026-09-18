package com.nlu.shared.domain.model;

public record StorageObjectMetadata(
        String key,
        long contentLength,
        String contentType,
        String etag
) {}
