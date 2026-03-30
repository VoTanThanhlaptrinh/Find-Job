package com.job_web.service.support.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.job_web.service.support.FileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    private static final String OCR_API_URL = "https://api.ocr.space/parse/image";

    private final RestClient restClient;
    private final String ocrApiKey;
    private final ObjectMapper objectMapper;

    public FileServiceImpl(
            RestClient.Builder restClientBuilder,
            @Value("${spring.ocr.api-key}") String ocrApiKey
    ) {
        this.restClient = restClientBuilder.build();
        this.ocrApiKey = ocrApiKey;
        this.objectMapper = new ObjectMapper();
    }

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

    @Override
    public String extractTextFromFileOcr(MultipartFile file) {
        try {
            byte[] fileBytes = file.getBytes();
            String fileName = file.getOriginalFilename();

            // Tạo resource từ byte array
            ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };

            // Build multipart body
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("file", fileResource)
                    .filename(fileName)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM);
            bodyBuilder.part("language", "eng");
            bodyBuilder.part("isOverlayRequired", "false");
            bodyBuilder.part("detectOrientation", "true");
            bodyBuilder.part("scale", "true");
            bodyBuilder.part("OCREngine", "2");

            // Gọi OCR.space API
            String response = restClient.post()
                    .uri(OCR_API_URL)
                    .header("apikey", ocrApiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(bodyBuilder.build())
                    .retrieve()
                    .body(String.class);

            return parseOcrResponse(response);

        } catch (Exception e) {
            log.error("Error calling OCR.space API: ", e);
            return "";
        }
    }

    private String parseOcrResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            // Kiểm tra lỗi từ OCR.space
            if (root.has("IsErroredOnProcessing") && root.get("IsErroredOnProcessing").asBoolean()) {
                String errorMessage = root.has("ErrorMessage")
                        ? root.get("ErrorMessage").toString()
                        : "Unknown OCR error";
                log.error("OCR.space returned error: {}", errorMessage);
                return "";
            }

            // Lấy text từ kết quả
            JsonNode parsedResults = root.get("ParsedResults");
            if (parsedResults != null && parsedResults.isArray() && !parsedResults.isEmpty()) {
                StringBuilder extractedText = new StringBuilder();
                for (JsonNode result : parsedResults) {
                    if (result.has("ParsedText")) {
                        extractedText.append(result.get("ParsedText").asText());
                    }
                }
                return extractedText.toString();
            }

            return "";
        } catch (Exception e) {
            log.error("Error parsing OCR response: ", e);
            return "";
        }
    }

    @Override
    public String cleanText(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return "";
        }
        String cleanText = rawText.replaceAll("\\\\s*", "");
        cleanText = cleanText.replaceAll("[ \t]+", " ");
        cleanText = cleanText.replaceAll("(?m)^[ \t]*\r?\n", "");
        return cleanText.trim();
    }
}
