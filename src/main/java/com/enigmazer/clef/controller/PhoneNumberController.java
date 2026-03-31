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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/phone")
@RequiredArgsConstructor
@Tag(name = "Phone number Management")
public class PhoneNumberController {

    private final PhoneNumberService phoneNumberService;

    // --- OTP Verification ---
    @PostMapping("/send-otp")
    @Operation(summary = "Send phone number verification otp")
    public ResponseEntity<Map<String,String>> sendOtp(
            @Valid @RequestBody SendOtpRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        phoneNumberService.sendOtp(request.phoneNumber(), principal.getId());
        return ResponseEntity.ok(Map.of("message", "OTP sent"));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP and add phone number")
    public ResponseEntity<Map<String,String>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        phoneNumberService.verifyOtp(principal.getId(),
                request.phoneNumber(),
                request.code());
        return ResponseEntity.ok(Map.of("message", "Phone number verified"));
    }

    // --- Phone Number Management ---
    @GetMapping
    @Operation(summary = "Get phone numbers of user")
    public ResponseEntity<List<PhoneResponse>> getUserPhoneNumbers(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(
                phoneNumberService.
                        getUserPhoneNumbers(principal.getId())
        );
    }

    @PostMapping("/set-primary")
    @Operation(summary = "Set secondary phone number as primary if 2fa is not enabled")
    public ResponseEntity<Map<String,String>> setPrimaryPhone(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        phoneNumberService.setPrimaryPhoneNumber(principal.getId());
        return ResponseEntity.ok(Map.of("message", "Phone Number set to primary successfully"));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete phone number if it's not in use for 2fa")
    public ResponseEntity<Map<String,String>> deletePhoneNumber(
            @Valid @RequestBody SendOtpRequest request, // reusing
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        phoneNumberService.deleteUserPhoneNumber(request.phoneNumber(), principal.getId());
        return ResponseEntity.ok(Map.of("message", "Phone Number deleted successfully"));
    }
}