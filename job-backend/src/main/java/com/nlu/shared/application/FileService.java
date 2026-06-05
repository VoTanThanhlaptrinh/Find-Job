package com.nlu.shared.application;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface FileService {
     String extractTextFromFile(InputStream inputStream);
     String extractTextFromFileOcr(MultipartFile file);
     String cleanText(String rawText);
     byte[] toByteArray(InputStream inputStream) throws IOException;
}
