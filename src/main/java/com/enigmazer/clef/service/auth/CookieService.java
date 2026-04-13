package com.enigmazer.clef.service.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CookieService {
    @Value("${jwt.refresh.cookie.expiration:7d}")
    private Duration refreshTokenExpiration;

    @Value("${jwt.temp.cookie.expiration:5m}")
    private Duration tempTokenExpiration;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    // --- Generate cookies ---
    public ResponseCookie generateRefreshTokenCookie(String token) {
        return ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(true)
                .path(contextPath + "/auth/")
                .maxAge(refreshTokenExpiration)
                .sameSite("Strict")
                .build();
    }

    public ResponseCookie generateTempTokenCookie(String token) {
        return ResponseCookie.from("tempToken", token)
                .httpOnly(true)
                .secure(true)
                .path(contextPath + "/auth/2fa/verify")
                .maxAge(tempTokenExpiration)
                .sameSite("Strict")
                .build();
    }

    // --- Clear cookies ---
    public ResponseCookie generateRefreshTokenClearCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path(contextPath + "/auth/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }

    public ResponseCookie generateTempTokenClearCookie() {
        return ResponseCookie.from("tempToken", "")
                .httpOnly(true)
                .secure(true)
                .path(contextPath + "/auth/2fa/verify")
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }
}