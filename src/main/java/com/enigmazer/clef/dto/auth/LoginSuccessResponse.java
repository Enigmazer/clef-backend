package com.enigmazer.clef.dto.auth;

public record LoginSuccessResponse(
        Long userId,
        String role,
        String message
) implements LoginResponse {}