package com.enigmazer.clef.controller;

import com.enigmazer.clef.config.security.CustomUserDetails;
import com.enigmazer.clef.dto.auth.AuthResponse;
import com.enigmazer.clef.dto.auth.LoginResponse;
import com.enigmazer.clef.dto.auth.LoginSuccessResponse;
import com.enigmazer.clef.dto.auth.TwoFAVerifyRequest;
import com.enigmazer.clef.service.auth.CookieService;
import com.enigmazer.clef.service.twofa.TwoFactorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/2fa")
@Tag(name = "Two Factor Authentication Management")
public class TwoFactorController {

    private final TwoFactorService twoFactorService;
    private final CookieService cookieService;

    @PostMapping("/send-otp")
    @Operation(summary = "Send two-factor authentication otp")
    public ResponseEntity<Void> sendOtp(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        twoFactorService.send2FAOtp(principal.getId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/enable")
    @Operation(summary = "Enable two-factor authentication")
    public ResponseEntity<Void> enable2FA(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody TwoFAVerifyRequest request
    ) {
        twoFactorService.enable2FA(principal.getId(), request.otpCode());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/disable")
    @Operation(summary = "Disable two-factor authentication")
    public ResponseEntity<Void> disable2FA(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody TwoFAVerifyRequest request
    ) {
        twoFactorService.disable2FA(principal.getId(), request.otpCode());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify OTP and complete 2FA login")
    public ResponseEntity<LoginResponse> verifyTwoFA(
            @CookieValue("tempToken") String tempToken,
            @Valid @RequestBody TwoFAVerifyRequest request
    ) {

        AuthResponse authData = twoFactorService.verifyTwoFA(tempToken, request.otpCode());

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
