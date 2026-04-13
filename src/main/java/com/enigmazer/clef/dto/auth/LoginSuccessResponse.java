package com.enigmazer.clef.dto.auth;

public record LoginSuccessResponse(
        Long userId,
        String role,
        String accessToken,
        String message
) implements LoginResponse {}