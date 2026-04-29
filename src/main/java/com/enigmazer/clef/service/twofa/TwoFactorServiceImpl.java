package com.enigmazer.clef.service.twofa;

import com.enigmazer.clef.dto.auth.AuthResponse;
import com.enigmazer.clef.dto.auth.TempTokenClaims;
import com.enigmazer.clef.entity.User;
import com.enigmazer.clef.exception.InvalidRequestException;
import com.enigmazer.clef.exception.SystemResourceNotFoundException;
import com.enigmazer.clef.repository.UserRepository;
import com.enigmazer.clef.service.auth.JwtService;
import com.enigmazer.clef.service.auth.RefreshTokenService;
import com.enigmazer.clef.service.phone.PhoneNumberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TwoFactorServiceImpl implements TwoFactorService{

    private final UserRepository userRepository;

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PhoneNumberService phoneNumberService;

    @Override
    @Transactional
    public void send2FAOtp(Long userId) {
        phoneNumberService.send2FAOtp(userId);
    }

    @Override
    @Transactional
    public void enable2FA(Long userId, String code) {
        User user = verify2FAOtpAndGetUser(code, userId);

        user.setTwoFactorEnabled(true);
        log.info("2FA enabled [userId={}]", userId);
    }

    @Override
    @Transactional
    public void disable2FA(Long userId, String code) {
        User user = verify2FAOtpAndGetUser(code, userId);

        user.setTwoFactorEnabled(false);
        log.info("2FA disabled [userId={}]", userId);
    }

    @Override
    @Transactional
    public AuthResponse verifyTwoFA(String tempToken, String otpCode) {
        if (!jwtService.isValidTempToken(tempToken)) {
            throw new InvalidRequestException("Invalid or expired 2FA session");
        }

        TempTokenClaims claims = jwtService.extractTempTokenClaims(tempToken);

        User user = verify2FAOtpAndGetUser(otpCode, claims.userId());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.generateRefreshToken(user);

        log.info("User successfully logged in via 2FA [userId={}]", user.getId());
        return new AuthResponse(user.getId(), user.getRole().getName().name(), accessToken, refreshToken);
    }

    // --- Helper Methods ---
    private User verify2FAOtpAndGetUser(String code, Long userId){
        phoneNumberService.verify2FAOtp(userId, code);

        return userRepository.findById(userId)
                .orElseThrow(() -> new SystemResourceNotFoundException("User not found", userId));
    }
}
