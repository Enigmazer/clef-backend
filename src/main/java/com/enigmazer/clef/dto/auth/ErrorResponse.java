package com.enigmazer.clef.dto.auth;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Builder
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        String reference,
        Map<String, String> fieldErrors
) {}