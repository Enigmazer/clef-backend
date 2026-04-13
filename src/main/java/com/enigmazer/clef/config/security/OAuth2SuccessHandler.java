package com.enigmazer.clef.config.security;

import com.enigmazer.clef.entity.User;
import com.enigmazer.clef.exception.SystemResourceNotFoundException;
import com.enigmazer.clef.repository.UserRepository;
import com.enigmazer.clef.service.auth.RefreshTokenService;
import com.enigmazer.clef.service.auth.CookieService;
import com.enigmazer.clef.service.auth.JwtService;
import com.enigmazer.clef.service.phone.PhoneNumberService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;

    private final CookieService cookieService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PhoneNumberService phoneNumberService;

    @Value("${frontend.url}")
    private String frontendUrl;
    @Value("${frontend.oauth2-redirect-path}")
    private String oAuth2RedirectPath;
    @Value("${frontend.oauth2-2fa-redirect-path}")
    private String oauth2TwoFaRedirectPath;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        log.info("OAuth2 authentication successful [email={}]", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new SystemResourceNotFoundException("User not found post oauth2 success for email", email));

        if (user.isTwoFactorEnabled()) {
            phoneNumberService.send2FAOtp(user.getId());
            String tempToken = jwtService.generateTempToken(user);
            ResponseCookie tempCookie = cookieService.generateTempTokenCookie(tempToken);
            response.addHeader(HttpHeaders.SET_COOKIE, tempCookie.toString());
            getRedirectStrategy().sendRedirect(request, response, frontendUrl + oauth2TwoFaRedirectPath);
            return;
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.generateRefreshToken(user);

        ResponseCookie refreshCookie = cookieService.generateRefreshTokenCookie(refreshToken);

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // # fragment is stripped by the browser before sending to server — never reaches servers or logs
        String targetUrl = frontendUrl + oAuth2RedirectPath + "#at=" + accessToken;
        log.debug("Redirecting user to frontend [targetUrl={}]", targetUrl.split("#")[0] + "#at=<redacted>");

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}