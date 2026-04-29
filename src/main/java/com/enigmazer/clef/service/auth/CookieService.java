package com.enigmazer.clef.service.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CookieService {
    @Value("${jwt.refresh.cookie.expiration:7d}")
    private Duration refreshCookieExpiration;

    @Value("${jwt.temp.cookie.expiration:5m}")
    private Duration tempCookieExpiration;

    @Value("${jwt.passwordResetIntent.cookie.expiration:5m}")
    private Duration passwordResetIntentCookieExpiration;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    // --- Generate cookies ---
    public ResponseCookie generateRefreshTokenCookie(String token) {
        return ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(true)
                .path(contextPath + "/auth/")
                .maxAge(refreshCookieExpiration)
                .sameSite("Strict")
                .build();
    }

    public ResponseCookie generateTempTokenCookie(String token) {
        return ResponseCookie.from("tempToken", token)
                .httpOnly(true)
                .secure(true)
                .path(contextPath + "/auth/2fa/verify")
                .maxAge(tempCookieExpiration)
                .sameSite("Strict")
                .build();
    }

    public ResponseCookie generatePasswordResetIntentCookie(){
        return ResponseCookie.from("passwordResetIntent", "true")
                .httpOnly(true)
                .secure(true)
                .path(contextPath + "/login/oauth2/code")
                .maxAge(passwordResetIntentCookieExpiration)
                .sameSite("Lax")
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

    public ResponseCookie generatePasswordResetIntentClearCookie() {
        return ResponseCookie.from("passwordResetIntent", "")
                .httpOnly(true)
                .secure(true)
                .path(contextPath + "/login/oauth2/code")
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }
}