package com.enigmazer.clef.exception;

import org.springframework.http.HttpStatus;

public class SystemResourceNotFoundException extends AppException {
    public SystemResourceNotFoundException(String message, Object resource) {
        super(
                message + " " + resource,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "SYSTEM_RESOURCE_MISSING"
        );
    }
}
