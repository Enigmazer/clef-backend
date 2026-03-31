package com.enigmazer.clef.dto.phone;

import com.enigmazer.clef.validation.ValidPhone;
import jakarta.validation.constraints.NotBlank;

public record SendOtpRequest(
        @NotBlank(message = "Phone Number can't be blank.")
        @ValidPhone(message = "Invalid Phone Number.")
        String phoneNumber
) {}