package com.job_web.service.support;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileService {
     String extractTextFromFile(InputStream inputStream);
     String extractTextFromFileOcr(MultipartFile file);
     String cleanText(String rawText);
}
