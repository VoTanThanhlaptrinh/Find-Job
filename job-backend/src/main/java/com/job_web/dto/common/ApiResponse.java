package com.job_web.dto.common;

public record ApiResponse<T>(
        String message,
        T data,
        int status
) {
    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public int getStatus() {
        return status;
    }
}
