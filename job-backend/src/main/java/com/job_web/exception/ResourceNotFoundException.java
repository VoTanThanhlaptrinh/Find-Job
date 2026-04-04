package com.job_web.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends AppException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
