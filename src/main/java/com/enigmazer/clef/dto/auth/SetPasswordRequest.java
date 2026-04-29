package com.enigmazer.clef.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SetPasswordRequest(
        @NotBlank(message = "Password is required")
        @Pattern(
                regexp = "^\\p{Print}{8,64}$",
                message = "Password must be at least 8 characters"
        )
        String password
) {}