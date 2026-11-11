package com.nlu.shared.infrastructure.client;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.nlu.shared.api.message.dto.FileServiceConfirmResponse;
import com.nlu.shared.api.message.dto.FileServicePresignResponse;
import com.nlu.shared.api.message.dto.FileServiceResponse;
import com.nlu.shared.api.message.dto.FileServiceReadUrlResponse;
import com.nlu.shared.application.FileServiceClient;

@Component
public class FileServiceClientImpl implements FileServiceClient {
    private static final ParameterizedTypeReference<FileServiceResponse<FileServicePresignResponse>> PRESIGN_TYPE =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<FileServiceResponse<FileServiceConfirmResponse>> CONFIRM_TYPE =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<FileServiceResponse<FileServiceReadUrlResponse>> READ_URL_TYPE =
            new ParameterizedTypeReference<>() { };

    private final RestClient fileServiceRestClient;
    private final RestClient fileDownloadRestClient;

    public FileServiceClientImpl(
            @Qualifier("fileServiceRestClient") RestClient fileServiceRestClient,
            @Qualifier("fileDownloadRestClient") RestClient fileDownloadRestClient) {
        this.fileServiceRestClient = fileServiceRestClient;
        this.fileDownloadRestClient = fileDownloadRestClient;
    }

    @Override
    public FileServicePresignResponse presign(String fileName, String mimeType, long size) {
        FileServiceResponse<FileServicePresignResponse> response = fileServiceRestClient.post()
                .uri("/api/v1/files/presign")
                .body(Map.of(
                        "context", "candidate-cv",
                        "file", Map.of("fileName", fileName, "mimeType", mimeType, "size", size)))
                .retrieve()
                .body(PRESIGN_TYPE);
        return requireData(response);
    }

    @Override
    public FileServiceConfirmResponse confirm(String tempKey) {
        FileServiceResponse<FileServiceConfirmResponse> response = fileServiceRestClient.post()
                .uri("/api/v1/files/confirm")
                .body(Map.of("tempKey", tempKey))
                .retrieve()
                .body(CONFIRM_TYPE);
        return requireData(response);
    }

    @Override
    public String readUrl(String permanentKey, String downloadFileName) {
        Map<String, String> request = new java.util.HashMap<>();
        request.put("key", permanentKey);
        if (downloadFileName != null && !downloadFileName.isBlank()) {
            request.put("downloadFileName", downloadFileName);
        }
        FileServiceResponse<FileServiceReadUrlResponse> response = fileServiceRestClient.post()
                .uri("/api/v1/files/read-url")
                .body(request)
                .retrieve()
                .body(READ_URL_TYPE);
        return requireData(response).readUrl();
    }

    @Override
    public void delete(String key) {
        fileServiceRestClient.method(HttpMethod.DELETE)
                .uri("/api/v1/files")
                .body(Map.of("key", key))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public byte[] download(String readUrl) {
        return fileDownloadRestClient.get().uri(readUrl).retrieve().body(byte[].class);
    }

    private <T> T requireData(FileServiceResponse<T> response) {
        if (response == null || !response.success() || response.data() == null) {
            throw new IllegalStateException(
                    response == null || response.message() == null
                            ? "File service returned an empty response"
                            : response.message());
        }
        return response.data();
    }
}
