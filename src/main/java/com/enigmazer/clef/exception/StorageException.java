package com.enigmazer.clef.exception;

import org.springframework.http.HttpStatus;

public class StorageException extends AppException {
    public StorageException(String message, Throwable cause) {
        super(
                message,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "STORAGE_ERROR"
        );
        initCause(cause);
    }
}