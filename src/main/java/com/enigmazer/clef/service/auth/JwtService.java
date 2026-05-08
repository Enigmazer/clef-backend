package com.enigmazer.clef.service.auth;

import com.enigmazer.clef.dto.auth.TokenClaims;
import com.enigmazer.clef.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access.cookie.expiration:15m}")
    private Duration accessTokenDuration;

    @Value("${jwt.temp.cookie.expiration:5m}")
    private Duration tempTokenDuration;

    @Value("${jwt.passwordReset.cookie.expiration:5m}")
    private Duration passwordResetTokenDuration;

    private SecretKey signingKey;

    @PostConstruct
    private void initKey() {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    public String generateAccessToken(User user){
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "ROLE_" + user.getRole().getName().name());
        claims.put("userId", user.getId());
        return buildToken(claims, user.getEmail(), accessTokenDuration);
    }

    public String generateTempToken(User user){
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "TWO_FA_PENDING");
        claims.put("userId", user.getId());
        return buildToken(claims, user.getEmail(), tempTokenDuration);
    }

    public String generatePasswordResetToken(User user){
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "PASSWORD_RESET");
        claims.put("userId", user.getId());
        return buildToken(claims, user.getEmail(), passwordResetTokenDuration);
    }

    private String buildToken(Map<String, Object> extraClaims, String email, Duration expiration){
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(signingKey)
                .compact();
    }

    public boolean isValidToken(String token, String userEmail){
        try {
            final String username = extractUsername(token);
            return (username.equals(userEmail)) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e){
            return false;
        }
    }

    public boolean isValidTempToken(String token){
        try {
            return !isTokenExpired(token) && is2FAPending(token);
        } catch (JwtException | IllegalArgumentException e){
            return false;
        }
    }

    public boolean isValidPasswordResetToken(String token){
        try {
            return !isTokenExpired(token) && isPasswordReset(token);
        } catch (JwtException | IllegalArgumentException e){
            return false;
        }
    }

    public TokenClaims extractClaims(String token){
        Claims claims = extractAllClaims(token);
        return new TokenClaims(
                claims.get("userId", Long.class),
                claims.getSubject(),
                claims.get("role", String.class)
        );
    }

    public String extractUsername(String token){
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token){
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    private boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token){
        return extractClaim(token, Claims::getExpiration);
    }

    private boolean is2FAPending(String token){
        return extractClaim(token, claims ->
                claims.get("type", String.class)).equals("TWO_FA_PENDING");
    }

    private boolean isPasswordReset(String token){
        return extractClaim(token, claims ->
                claims.get("type", String.class)).equals("PASSWORD_RESET");
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver){
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token){
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
