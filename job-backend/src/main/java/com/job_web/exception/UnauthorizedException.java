package com.job_web.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends AppException {
    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
