package com.enigmazer.clef.controller;

import com.enigmazer.clef.config.security.CustomUserDetails;
import com.enigmazer.clef.dto.auth.SetPasswordRequest;
import com.enigmazer.clef.dto.auth.UpdatePasswordRequest;
import com.enigmazer.clef.service.auth.CookieService;
import com.enigmazer.clef.service.password.PasswordService;
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

    @GetMapping("/reset/oauth2/init")
    @Operation(summary = "Initialize reset password flow through oauth2")
    public ResponseEntity<Void> initOAuth2PasswordReset() {
        ResponseCookie intentCookie = cookieService.generatePasswordResetIntentCookie();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, intentCookie.toString())
                .header(HttpHeaders.LOCATION, "/api/oauth2/authorization/google")
                .build();
    }
}
