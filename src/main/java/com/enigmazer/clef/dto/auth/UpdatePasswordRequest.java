package com.enigmazer.clef.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdatePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Pattern(
                regexp = "^\\p{Print}{8,64}$",
                message = "New password must be at least 8 characters"
        )
        String newPassword
) {}