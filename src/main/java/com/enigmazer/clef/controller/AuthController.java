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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication Management")
public class AuthController {

    private final AuthService authService;
    private final CookieService cookieService;

    // --- Authentication Operations ---
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

    @PostMapping("/password")
    @Operation(summary = "Set or update password for user account")
    public ResponseEntity<Map<String, String>> setOrUpdatePassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody SetUpdatePasswordReq request) {

        authService.setOrUpdatePassword(principal.getId(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password set successfully."));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Generate new access and refresh tokens")
    public ResponseEntity<Map<String, String>> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {

        if(refreshToken == null || refreshToken.isBlank()){
            throw new InvalidRequestException("No refresh token found.");
        }

        TokenPair tokens = authService.refreshTokens(refreshToken);
        ResponseCookie newRefreshCookie = cookieService.generateRefreshTokenCookie(tokens.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, newRefreshCookie.toString())
                .body(Map.of("message", "Tokens refreshed successfully",
                             "accessToken", tokens.accessToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout from current session")
    public ResponseEntity<Map<String, String>> logout(
            @AuthenticationPrincipal CustomUserDetails principal,
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {

        if(principal != null && refreshToken != null){
            authService.logout(principal.getId(), refreshToken);
        }

        ResponseCookie logoutCookie = cookieService.generateRefreshTokenClearCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, logoutCookie.toString())
                .body(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout from all sessions")
    public ResponseEntity<Map<String, String>> logoutAll(
            @AuthenticationPrincipal CustomUserDetails principal) {
        authService.logoutAllDevices(principal.getId());

        ResponseCookie logoutCookie = cookieService.generateRefreshTokenClearCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, logoutCookie.toString())
                .body(Map.of("message", "Logged out from all devices successfully"));
    }

    // --- Two-Factor Authentication Operations ---
    @PostMapping("/2fa/send-otp")
    @Operation(summary = "Send two-factor authentication otp")
    public ResponseEntity<Void> sendOtp(
            @AuthenticationPrincipal CustomUserDetails principal) {
        authService.send2FAOtp(principal.getId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/2fa/enable")
    @Operation(summary = "Enable two-factor authentication")
    public ResponseEntity<Void> enable2FA(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody TwoFAVerifyRequest request) {
        authService.enable2FA(principal.getId(), request.otpCode());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/2fa/disable")
    @Operation(summary = "Disable two-factor authentication")
    public ResponseEntity<Void> disable2FA(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody TwoFAVerifyRequest request) {
        authService.disable2FA(principal.getId(), request.otpCode());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/2fa/verify")
    @Operation(summary = "Verify OTP and complete 2FA login")
    public ResponseEntity<LoginResponse> verifyTwoFA(
            @CookieValue("tempToken") String tempToken,
            @Valid @RequestBody TwoFAVerifyRequest request) {

        AuthResponse authData = authService.verifyTwoFA(tempToken, request.otpCode());

        ResponseCookie refreshCookie = cookieService.generateRefreshTokenCookie(authData.refreshToken());
        ResponseCookie clearTempCookie = cookieService.generateTempTokenClearCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearTempCookie.toString())
                .body(new LoginSuccessResponse(
                        authData.userId(), authData.role(),
                        authData.accessToken(), "Login successful"));
    }
}