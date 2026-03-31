package com.enigmazer.clef.dto.phone;

import java.time.Instant;

public record PhoneResponse(
        Long id,
        String phoneNumber,
        boolean primary,
        Instant createdAt
) {}