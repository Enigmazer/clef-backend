package com.enigmazer.clef.controller;

import com.enigmazer.clef.config.security.CustomUserDetails;
import com.enigmazer.clef.dto.phone.PhoneResponse;
import com.enigmazer.clef.dto.phone.SendOtpRequest;
import com.enigmazer.clef.dto.phone.VerifyOtpRequest;
import com.enigmazer.clef.service.phone.PhoneNumberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/phone")
@Tag(name = "Phone number Management")
public class PhoneNumberController {

    private final PhoneNumberService phoneNumberService;

    // --- OTP Verification ---
    @PostMapping("/send-otp")
    @Operation(summary = "Send phone number verification otp")
    public ResponseEntity<Void> sendOtp(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody SendOtpRequest request
    ) {
        phoneNumberService.sendOtp(request.phoneNumber(), principal.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP and add phone number")
    public ResponseEntity<Void> verifyOtp(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody VerifyOtpRequest request
    ) {
        phoneNumberService.verifyOtp(principal.getId(),
                request.phoneNumber(),
                request.code());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // --- Phone Number Management ---
    @GetMapping
    @Operation(summary = "Get phone numbers of user")
    public ResponseEntity<List<PhoneResponse>> getUserPhoneNumbers(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(
                phoneNumberService.getUserPhoneNumbers(principal.getId())
        );
    }

    @PostMapping("/set-primary")
    @Operation(summary = "Set secondary phone number as primary if 2fa is not enabled")
    public ResponseEntity<Void> setPrimaryPhone(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        phoneNumberService.setPrimaryPhoneNumber(principal.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete phone number if it's not in use for 2fa")
    public ResponseEntity<Void> deletePhoneNumber(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody SendOtpRequest request // reusing
    ) {
        phoneNumberService.deleteUserPhoneNumber(request.phoneNumber(), principal.getId());
        return ResponseEntity.noContent().build();
    }
}