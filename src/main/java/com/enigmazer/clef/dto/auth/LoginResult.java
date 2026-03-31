package com.enigmazer.clef.dto.auth;

public sealed interface LoginResult permits LoginResult.FullAuth, LoginResult.TwoFactorPending {
    record FullAuth(
            AuthResponse authResponse
    ) implements LoginResult {}

    record TwoFactorPending(
            Long userId,
            String tempToken
    ) implements LoginResult {}
}