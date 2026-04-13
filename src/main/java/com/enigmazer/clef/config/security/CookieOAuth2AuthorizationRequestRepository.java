package com.enigmazer.clef.config.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * Stores the OAuth2 authorization request (state + nonce) in a short-lived
 * httpOnly cookie instead of the HTTP session.
 *
 * Required because the app is STATELESS (no session), so the default
 * HttpSessionOAuth2AuthorizationRequestRepository would fail in production —
 * it can't find the saved state on the callback because no session exists.
 *
 * SameSite=Lax is intentional: this cookie is only involved in top-level
 * browser navigation (redirect to provider and back), not in cross-site XHR.
 * Lax allows the cookie to be sent on top-level GET navigations.
 */
@Component
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String COOKIE_NAME = "oauth2_auth_request";
    private static final int COOKIE_TTL_SECONDS = 180; // 3 min — enough for provider redirect round-trip

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getCookie(request)
                .map(c -> (OAuth2AuthorizationRequest)
                        SerializationUtils.deserialize(
                                Base64.getUrlDecoder().decode(c.getValue())))
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(response);
            return;
        }
        String value = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(SerializationUtils.serialize(authorizationRequest));
        addCookie(response, value, COOKIE_TTL_SECONDS);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest oAuth2AuthorizationRequest = loadAuthorizationRequest(request);
        deleteCookie(response);
        return oAuth2AuthorizationRequest;
    }

    // --- Helper Methods ---
    private void addCookie(HttpServletResponse response, String value, int maxAge) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void deleteCookie(HttpServletResponse response) {
        addCookie(response, "", 0);
    }

    private Optional<Cookie> getCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .findFirst();
    }
}
