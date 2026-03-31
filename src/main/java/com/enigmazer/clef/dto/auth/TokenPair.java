package com.enigmazer.clef.dto.auth;

public record TokenPair(
        String accessToken,
        String refreshToken
) {}