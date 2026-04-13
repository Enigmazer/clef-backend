package com.enigmazer.clef.service.auth;

import com.enigmazer.clef.entity.RefreshToken;
import com.enigmazer.clef.entity.User;
import com.enigmazer.clef.exception.BusinessException;
import com.enigmazer.clef.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh.cookie.expiration:7d}")
    private Duration refreshTokenDuration;

    @Transactional
    public String generateRefreshToken(User user) {
        // Double UUID for extra entropy — single UUID (122 bits) is sufficient but this adds defense in depth
        String randomTokenString = UUID.randomUUID() + "-" + UUID.randomUUID();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(randomTokenString)
                .expiryDate(Instant.now().plus(refreshTokenDuration))
                .build();

        refreshTokenRepository.save(refreshToken);

        log.debug("Created new refresh token [userId={}]", user.getId());
        return randomTokenString;
    }

    @Transactional
    public void verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            log.warn("Deleted expired refresh token for user [userId={}]", token.getUser().getEmail());
            throw new BusinessException("Session expired. Please sign in again");
        }
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("Deleted all refresh tokens for user [userId={}]", userId);
    }

    @Transactional
    public void deleteByToken(String refreshTokenString) {
        refreshTokenRepository.deleteByToken(refreshTokenString);
    }

    // This method is the only way for clearing expired refresh tokens
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        log.info("Running scheduled garbage collection for expired refresh tokens...");

        int deletedCount = refreshTokenRepository.deleteAllExpiredSince(Instant.now());

        if (deletedCount > 0) {
            log.info("Successfully purged {} expired refresh tokens from the database.", deletedCount);
        } else {
            log.debug("No expired refresh tokens found in database.");
        }
    }
}