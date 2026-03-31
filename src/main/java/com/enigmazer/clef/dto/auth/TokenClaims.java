package com.enigmazer.clef.dto.auth;

public record TokenClaims(
        Long userId,
        String email,
        String role
) {}