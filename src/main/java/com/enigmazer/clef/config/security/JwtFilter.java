package com.enigmazer.clef.config.security;

import com.enigmazer.clef.dto.auth.TokenClaims;
import com.enigmazer.clef.service.auth.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final HandlerExceptionResolver exceptionResolver;
    private final WebAuthenticationDetailsSource detailsSource = new WebAuthenticationDetailsSource();

    public JwtFilter(
            JwtService jwtService,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver
    ) {
        this.jwtService = jwtService;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request){
        String path = request.getServletPath();
        // These paths manage token issuance/rotation and must be reachable without a valid access token
        return path.startsWith("/auth/login")
                || path.startsWith("/auth/2fa/verify")
                || path.startsWith("/auth/refresh")
                || path.startsWith("/oauth2");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = null;

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            TokenClaims tokenClaims = jwtService.extractClaims(token);
            String email = tokenClaims.email();

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                if (jwtService.isValidToken(token, email)) {
                    UsernamePasswordAuthenticationToken authToken = getUsernamePasswordAuthenticationToken(tokenClaims, email);

                    authToken.setDetails(detailsSource.buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("User authenticated successfully via HttpOnly cookie [email={}]", email);
                }
            }

            filterChain.doFilter(request, response);

        }  catch (ExpiredJwtException | MalformedJwtException e) {
            log.warn("JWT validation failed [path={}, reason={}]", request.getServletPath(), e.getMessage());
            // Delegate to @ControllerAdvice instead of writing directly to the response
            exceptionResolver.resolveException(request, response, null, e);
        } catch (Exception e) {
            log.error("Unexpected error during JWT validation [path={}]", request.getServletPath(), e);
            exceptionResolver.resolveException(request, response, null, e);
        }
    }

    private static UsernamePasswordAuthenticationToken getUsernamePasswordAuthenticationToken(TokenClaims tokenClaims, String email) {
        String role = tokenClaims.role();

        CustomUserDetails userDetails = new CustomUserDetails(
                tokenClaims.userId(),
                email,
                "",
                true,
                List.of(new SimpleGrantedAuthority(role))
        );

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }
}