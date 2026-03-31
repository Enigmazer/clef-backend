package com.enigmazer.clef.dto.auth;

public record AuthResponse(
        Long userId,
        String role,
        String accessToken,
        String refreshToken
) {}