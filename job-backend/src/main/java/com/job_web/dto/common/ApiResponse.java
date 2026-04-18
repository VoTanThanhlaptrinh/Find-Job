package com.job_web.dto.common;

public record ApiResponse<T>(
        String message,
        T data,
        int status,
        String traceId
) {
    public ApiResponse(String message, T data, int status) {
        this(message, data, status, null);
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public int getStatus() {
        return status;
    }

    public String getTraceId() {
        return traceId;
    }
}
