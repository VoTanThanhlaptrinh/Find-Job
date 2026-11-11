package com.nlu.shared.application;

import com.nlu.shared.api.message.dto.FileServiceConfirmResponse;
import com.nlu.shared.api.message.dto.FileServicePresignResponse;

public interface FileServiceClient {
    FileServicePresignResponse presign(String fileName, String mimeType, long size);

    FileServiceConfirmResponse confirm(String tempKey);

    String readUrl(String permanentKey, String downloadFileName);

    void delete(String key);

    byte[] download(String readUrl);
}
