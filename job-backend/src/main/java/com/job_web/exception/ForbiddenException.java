package com.job_web.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends AppException {
    private static final long serialVersionUID = 1L;

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
