package com.enigmazer.clef.controller;

import com.enigmazer.clef.config.security.CustomUserDetails;
import com.enigmazer.clef.dto.auth.SetPasswordRequest;
import com.enigmazer.clef.dto.auth.UpdatePasswordRequest;
import com.enigmazer.clef.enums.SocialAccountProvider;
import com.enigmazer.clef.exception.InvalidRequestException;
import com.enigmazer.clef.service.auth.CookieService;
import com.enigmazer.clef.service.password.PasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/password")
@Tag(name = "Password Management")
public class PasswordController {

    private final PasswordService passwordService;
    private final CookieService cookieService;

    @PostMapping("/set")
    @Operation(summary = "Set password for your account")
    public ResponseEntity<Void> setPassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody SetPasswordRequest request
    ) {
        passwordService.setPassword(request, principal.getId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/update")
    @Operation(summary = "Update password of your account")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpdatePasswordRequest request
    ) {
        passwordService.updatePassword(request, principal.getId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/reset")
    @Operation(summary = "Reset your account password")
    public ResponseEntity<Void> resetPassword(
            @CookieValue(name = "passwordReset", required = true) String passwordResetToken,
            @Valid @RequestBody SetPasswordRequest request  // reusing
    ){
        passwordService.resetPassword(passwordResetToken, request);

        ResponseCookie resetPasswordClearCookie = cookieService.generatePasswordResetClearCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, resetPasswordClearCookie.toString())
                .build();
    }

    @GetMapping("/reset/oauth2/init/{provider}")
    @Operation(summary = "Initialize reset password flow through oauth2")
    public ResponseEntity<Void> initOAuth2PasswordReset(
            @PathVariable("provider") String rawProvider
    ) {
        String provider;
        try {
            provider = SocialAccountProvider.valueOf(rawProvider.toUpperCase()).name().toLowerCase();
        } catch (IllegalArgumentException e) {
            log.warn("Password reset init attempt with unsupported provider [provider={}]", rawProvider);
            throw new InvalidRequestException("Unsupported provider");
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/api/oauth2/authorization/" + provider + "-reset")
                .build();
    }
}
