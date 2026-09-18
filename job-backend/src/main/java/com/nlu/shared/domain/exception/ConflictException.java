package com.nlu.shared.domain.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends AppException {
    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
