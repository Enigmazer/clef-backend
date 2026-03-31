package com.enigmazer.clef.dto.auth;

public record TempTokenClaims(
        Long userId,
        String email
) {}
