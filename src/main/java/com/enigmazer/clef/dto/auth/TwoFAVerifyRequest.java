package com.enigmazer.clef.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TwoFAVerifyRequest(
        @NotBlank
        @Size(min = 6, max = 6)
        String otpCode
) {}