package com.enigmazer.clef.service.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class CookieService {

    @Value("${jwt.access.cookie.expiration:15m}")
    private Duration accessTokenExpiration;

    @Value("${jwt.refresh.cookie.expiration:7d}")
    private Duration refreshTokenExpiration;

    @Value("${jwt.temp.cookie.expiration:5m}")
    private Duration tempTokenExpiration;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    // --- Generate cookies ---
    public ResponseCookie generateAccessTokenCookie(String token) {
        return ResponseCookie.from("accessToken", token)
                .httpOnly(true)
                .secure(true)     // Required for SameSite="None"
                .path("/")
                .maxAge(accessTokenExpiration)
                .sameSite("None") // Required for cross-domain
                .build();
    }

    public ResponseCookie generateRefreshTokenCookie(String token) {
        return ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(true)
                .path(contextPath + "/auth/")
                .maxAge(refreshTokenExpiration)
                .sameSite("None")
                .build();
    }

    public ResponseCookie generateTempTokenCookie(String token) {
        return ResponseCookie.from("tempToken", token)
                .httpOnly(true)
                .secure(true)
                .path(contextPath + "/auth/2fa/verify")
                .maxAge(tempTokenExpiration)
                .sameSite("None")
                .build();
    }

    // --- Clear cookies ---
    public ResponseCookie generateTempTokenClearCookie() {
        return ResponseCookie.from("tempToken", "")
                .httpOnly(true)
                .secure(true)
                .path(contextPath + "/auth/2fa/verify")
                .maxAge(0)
                .sameSite("None")
                .build();
    }

    public List<ResponseCookie> generateLogoutCookies() {
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true).secure(true).path("/").maxAge(0).sameSite("None").build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(true).path(contextPath + "/auth/").maxAge(0).sameSite("None").build();

        return List.of(accessCookie, refreshCookie);
    }
}