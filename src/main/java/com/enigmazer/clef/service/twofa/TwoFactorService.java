package com.enigmazer.clef.service.twofa;

import com.enigmazer.clef.dto.auth.AuthResponse;

public interface TwoFactorService {
    void send2FAOtp(Long userId);

    void enable2FA(Long userId, String code);

    void disable2FA(Long userId, String code);

    AuthResponse verifyTwoFA(String tempToken, String otpCode);
}
