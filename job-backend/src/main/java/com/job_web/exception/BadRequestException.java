package com.job_web.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends AppException {
    private static final long serialVersionUID = 1L;

    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
