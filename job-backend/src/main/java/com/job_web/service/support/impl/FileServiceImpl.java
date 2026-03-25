package com.job_web.service.support.impl;

import com.job_web.service.support.FileService;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class FileServiceImpl implements FileService {
    @Override
    public String extractTextFromFile(InputStream inputStream) {
        try {
            Tika tika = new Tika();
            // Tika sẽ tự nhận diện đây là PDF hay DOCX và trả về text
            return tika.parseToString(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
