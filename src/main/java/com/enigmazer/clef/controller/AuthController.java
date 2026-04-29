package com.enigmazer.clef.controller;

import com.enigmazer.clef.config.security.CustomUserDetails;
import com.enigmazer.clef.dto.auth.*;
import com.enigmazer.clef.exception.InvalidRequestException;
import com.enigmazer.clef.service.auth.AuthService;
import com.enigmazer.clef.service.auth.CookieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Authentication Management")
public class AuthController {

    private final AuthService authService;
    private final CookieService cookieService;

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return switch (authService.authenticateUser(request.email(), request.password())) {
            case LoginResult.FullAuth(AuthResponse authResponse) -> {
                ResponseCookie refreshCookie = cookieService
                        .generateRefreshTokenCookie(authResponse.refreshToken());
                yield ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                        .body(new LoginSuccessResponse(
                                authResponse.userId(), authResponse.role(),
                                authResponse.accessToken(), "Login successful"));
            }
            case LoginResult.TwoFactorPending pending -> {
                ResponseCookie tempCookie = cookieService.generateTempTokenCookie(pending.tempToken());
                yield ResponseEntity.status(HttpStatus.ACCEPTED)
                        .header(HttpHeaders.SET_COOKIE, tempCookie.toString())
                        .body(new TwoFactorRequiredResponse(true));
            }
        };
    }

    @PostMapping("/refresh")
    @Operation(summary = "Generate new access and refresh tokens")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {

        if(refreshToken == null || refreshToken.isBlank()){
            throw new InvalidRequestException("No refresh token found.");
        }

        TokenPair tokens = authService.refreshTokens(refreshToken);
        ResponseCookie newRefreshCookie = cookieService.generateRefreshTokenCookie(tokens.refreshToken());
        AccessTokenResponse accessToken = new AccessTokenResponse(tokens.accessToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, newRefreshCookie.toString())
                .body(accessToken);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout from current session")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        if(refreshToken != null){
            authService.logout(refreshToken);
        }

        ResponseCookie logoutCookie = cookieService.generateRefreshTokenClearCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, logoutCookie.toString()).build();
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout from all sessions")
    public ResponseEntity<Void> logoutAll(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        authService.logoutAllDevices(principal.getId());

        ResponseCookie logoutCookie = cookieService.generateRefreshTokenClearCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, logoutCookie.toString()).build();
    }
}