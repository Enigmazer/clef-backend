package com.enigmazer.clef.service.auth;

import com.enigmazer.clef.dto.auth.AuthResponse;
import com.enigmazer.clef.dto.auth.LoginResult;
import com.enigmazer.clef.dto.auth.TokenPair;

public interface AuthService {
    LoginResult authenticateUser(String email, String password);

    void setOrUpdatePassword(Long userId, String newPassword);

    TokenPair refreshTokens(String refreshToken);

    void logout(Long userId, String refreshToken);

    void logoutAllDevices(Long userId);

    void send2FAOtp(Long userId);

    void enable2FA(Long userId, String code);

    void disable2FA(Long userId, String code);

    AuthResponse verifyTwoFA(String tempToken, String otpCode);
}
