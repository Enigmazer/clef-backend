package com.enigmazer.clef.service.auth;

import com.enigmazer.clef.dto.auth.LoginResult;
import com.enigmazer.clef.dto.auth.TokenPair;

public interface AuthService {
    LoginResult authenticateUser(String email, String password);

    void clearPassword(Long userId);

    TokenPair refreshTokens(String refreshToken);

    void logout(String refreshToken);

    void logoutAllDevices(Long userId);
}
