package com.enigmazer.clef.dto.auth;

public record TwoFactorRequiredResponse(
        boolean twoFactorRequired
) implements LoginResponse {}