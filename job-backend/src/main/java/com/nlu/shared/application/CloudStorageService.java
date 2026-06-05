package com.nlu.shared.application;

public interface CloudStorageService {
    void uploadFile(byte[] data, String key, String originalName);
}
