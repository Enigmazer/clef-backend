package com.enigmazer.clef.service.auth.impl;

import com.enigmazer.clef.config.security.CustomUserDetails;
import com.enigmazer.clef.dto.auth.AuthResponse;
import com.enigmazer.clef.dto.auth.LoginResult;
import com.enigmazer.clef.dto.auth.TempTokenClaims;
import com.enigmazer.clef.dto.auth.TokenPair;
import com.enigmazer.clef.entity.RefreshToken;
import com.enigmazer.clef.entity.User;
import com.enigmazer.clef.exception.*;
import com.enigmazer.clef.repository.UserRepository;
import com.enigmazer.clef.service.auth.AuthService;
import com.enigmazer.clef.service.auth.JwtService;
import com.enigmazer.clef.service.auth.RefreshTokenService;
import com.enigmazer.clef.service.phone.PhoneNumberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

    private final UserRepository userRepository;

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PhoneNumberService phoneNumberService;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;


    @Override
    @Transactional
    public LoginResult authenticateUser(String email, String password) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
        }catch (DisabledException ex){
            log.warn("Disabled user account login attempt " +
                    "[email={}]", email);
            throw new InvalidRequestException("Account is disabled");
        }catch (AuthenticationException ex){
            throw new InvalidRequestException("Invalid email or password");
        }

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();

        Long userId = principal.getId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SystemResourceNotFoundException("User not found post-auth", userId));

        if (user.isTwoFactorEnabled()) {
            log.info("User account 2fa login pending [userId={}]", userId);
            send2FAOtp(user.getId());
            String tempToken = jwtService.generateTempToken(user);
            return new LoginResult.TwoFactorPending(
                    user.getId(),
                    tempToken
            );
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.generateRefreshToken(user);

        log.info("User successfully signed in via email and password [userId={}]", userId);
        return new LoginResult.FullAuth(
                new AuthResponse(
                        user.getId(),
                        user.getRole().getName().name(),
                        accessToken,
                        refreshToken
                )
        );
    }

    @Override
    @Transactional
    public void setOrUpdatePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if(passwordEncoder.matches(newPassword, user.getPassword())){
            throw new BusinessException("New password must be different from your current password.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password successfully set [userId={}]", userId);
    }

    @Override
    @Transactional
    public TokenPair refreshTokens(String refreshToken) {
        RefreshToken token = refreshTokenService.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException("Invalid or expired refresh token."));

        //  deletes the token and throws BusinessException if expired
        refreshTokenService.verifyExpiration(token);

        User user = token.getUser();

        refreshTokenService.deleteByToken(refreshToken);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = refreshTokenService.generateRefreshToken(user);

        log.info("Tokens refreshed [userId={}]", user.getId());
        return new TokenPair(newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void logout(Long userId, String refreshToken) {
        refreshTokenService.deleteByToken(refreshToken);
        log.info("User logged out from current device [userId={}]", userId);
    }

    @Override
    @Transactional
    public void logoutAllDevices(Long userId){
        refreshTokenService.deleteByUserId(userId);
        log.info("User logged out from all devices [userId={}]", userId);
    }

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
        userRepository.save(user);
        log.info("2FA enabled [userId={}]", userId);
    }

    @Override
    @Transactional
    public void disable2FA(Long userId, String code) {
        User user = verify2FAOtpAndGetUser(code, userId);

        user.setTwoFactorEnabled(false);
        userRepository.save(user);
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

        log.info("User successfully signed in via 2FA [userId={}]", user.getId());
        return new AuthResponse(user.getId(), user.getRole().getName().name(), accessToken, refreshToken);
    }

    private User verify2FAOtpAndGetUser(String code, Long userId){
        phoneNumberService.verify2FAOtp(userId, code);

        return userRepository.findById(userId)
                .orElseThrow(() -> new SystemResourceNotFoundException("User not found", userId));
    }
}