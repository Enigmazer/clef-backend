package com.enigmazer.clef.dto.phone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyOtpRequest(
        @NotBlank
        String phoneNumber,

        @NotBlank
        @Size(min = 6, max = 6)
        String code
) {}