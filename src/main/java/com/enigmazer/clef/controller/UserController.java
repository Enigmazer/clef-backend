package com.enigmazer.clef.controller;

import com.enigmazer.clef.config.security.CustomUserDetails;
import com.enigmazer.clef.dto.user.UserResponse;
import com.enigmazer.clef.service.user.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Tag(name = "User management")
public class UserController {

    private final UserService userService;

    // --- Get one mapping ---
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(
            @AuthenticationPrincipal CustomUserDetails principal
    ){
        return ResponseEntity.ok()
                .body(userService.getCurrentUser(principal.getId()));
    }

    // --- Patch Mapping ---
    @PatchMapping("/preferences/phone-visibility/toggle")
    public ResponseEntity<Boolean> toggleUserPhoneVisibility(
            @AuthenticationPrincipal CustomUserDetails principal
    ){
        return ResponseEntity.ok()
                .body(userService.toggleUserPhoneVisibility(principal.getId()));
    }

    @PatchMapping("/preferences/student-section-visibility/toggle")
    public ResponseEntity<Boolean> toggleUserStudentSectionVisibility(
            @AuthenticationPrincipal CustomUserDetails principal
    ){
        return ResponseEntity.ok()
                .body(userService.toggleUserStudentSectionVisibility(principal.getId()));
    }

    @PatchMapping("/preferences/teacher-section-visibility/toggle")
    public ResponseEntity<Boolean> toggleUserTeacherSectionVisibility(
            @AuthenticationPrincipal CustomUserDetails principal
    ){
        return ResponseEntity.ok()
                .body(userService.toggleUserTeacherSectionVisibility(principal.getId()));
    }
}
