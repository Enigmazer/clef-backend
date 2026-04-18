package com.enigmazer.clef.exception;

import com.enigmazer.clef.dto.auth.ErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // --- Custom ----
    /**
     * Catches all custom exceptions (ResourceNotFoundException,
     * SystemResourceNotFoundException, BusinessException, etc.)
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(
            AppException ex, HttpServletRequest request) {

        if (ex.getStatus().is5xxServerError()) {
            log.error("System exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        } else {
            log.warn("Client exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        }

        return generateResponse(ex.getStatus(), ex.getMessage(), request);
    }

    // --- Security ----
    /**
     * Catches 401: unauthenticated request.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {

        log.warn("Unauthorized access attempt on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return generateResponse(HttpStatus.UNAUTHORIZED, "Authentication required.", request);
    }

    /**
     * Catches 401: invalid or expired jwt.
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(
            JwtException ex, HttpServletRequest request) {

        log.warn("JWT validation failed on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return generateResponse(HttpStatus.UNAUTHORIZED, "Invalid or expired token.", request);
    }


    /**
     * Catches 403: authenticated but not allowed.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("Access denied on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return generateResponse(HttpStatus.FORBIDDEN, "Access denied.", request);
    }

    private static final Set<String> KNOWN_CODES = Set.of(
            "unverified_email", "account_disabled",
            "missing_email", "unsupported_provider", "missing_github_email"
    );

    /**
     * Catches OAuth2 authentication failures — e.g. unsupported provider, unverified email.
     */
    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleOAuth2AuthenticationException(
            OAuth2AuthenticationException ex, HttpServletRequest request) {

        String errorCode = ex.getError().getErrorCode();

        // Only expose custom messages from exceptions
        String message = KNOWN_CODES.contains(errorCode)
                ? ex.getMessage()
                : "OAuth2 authentication failed.";

        log.warn("OAuth2 failure [{}] on [{}]", errorCode, request.getRequestURI());
        return generateResponse(HttpStatus.UNAUTHORIZED, message, request);
    }

    // --- Validation ---
    /**
     * Catches @Valid failures on request body DTOs.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("Validation failed on [{}]: {}", request.getRequestURI(), fieldErrors);
        return generateResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    /**
     * Catches @Validated failures on @RequestParam / @PathVariable.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = violation.getPropertyPath().toString();
            fieldErrors.put(field, violation.getMessage());
        }

        log.warn("Constraint violation on [{}]: {}", request.getRequestURI(), fieldErrors);
        return generateResponse(HttpStatus.BAD_REQUEST, "Validation failed.", request, fieldErrors);
    }

    /**
     * When someone passes the wrong type to path variable
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Type mismatch on [{}]: parameter '{}' expected type {}",
                request.getRequestURI(), ex.getName(), Objects.requireNonNull(ex.getRequiredType()).getSimpleName());
        return generateResponse(HttpStatus.BAD_REQUEST,
                "Invalid value for parameter: " + ex.getName(), request);
    }

    // --- Http/request ---
    /**
     * Catches wrong HTTP method — e.g. POST on a GET-only endpoint.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        log.warn("Method not allowed on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return generateResponse(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported.", request);
    }

    /**
     * Catches unreadable or malformed JSON body.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Malformed request body on [{}]: {}", request.getRequestURI(), ex.getMessage());
        return generateResponse(HttpStatus.BAD_REQUEST, "Malformed or missing request body.", request);
    }

    /**
     * Catches 404: requests to non-existent endpoints
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("No endpoint found for [{}] {}", request.getMethod(), request.getRequestURI());
        return generateResponse(HttpStatus.NOT_FOUND, "The requested endpoint does not exist.", request);
    }

    // --- Absolute fallback ---
    /**
     * Catches anything not handled above.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception on [{}]: ", request.getRequestURI(), ex);
        return generateResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request);
    }

    // --- Helper Methods ---
    private ResponseEntity<ErrorResponse> generateResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors) {

        ErrorResponse body = ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .reference(UUID.randomUUID().toString())
                .fieldErrors(fieldErrors)
                .build();

        return new ResponseEntity<>(body, status);
    }

    private ResponseEntity<ErrorResponse> generateResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request) {
        return generateResponse(status, message, request, null);
    }
}