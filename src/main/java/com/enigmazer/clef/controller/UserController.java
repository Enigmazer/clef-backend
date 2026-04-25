package com.enigmazer.clef.controller;

import com.enigmazer.clef.config.security.CustomUserDetails;
import com.enigmazer.clef.dto.user.AvatarUploadResponse;
import com.enigmazer.clef.dto.user.UserPreferenceUpdateResponse;
import com.enigmazer.clef.dto.user.UserResponse;
import com.enigmazer.clef.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Tag(name = "User management")
public class UserController {

    private final UserService userService;

    // --- Get one mapping ---
    @GetMapping("/me")
    @Operation(summary = "Return the info of currently logged in user")
    public ResponseEntity<UserResponse> getMe(
            @AuthenticationPrincipal CustomUserDetails principal
    ){
        return ResponseEntity.ok(userService.getCurrentUser(principal.getId()));
    }

    // --- Patch Mapping ---
    @PatchMapping("/preferences/phone-visibility/toggle")
    @Operation(summary = "Toggle the visibility of users phone number to students")
    public ResponseEntity<UserPreferenceUpdateResponse> toggleUserPhoneVisibility(
            @AuthenticationPrincipal CustomUserDetails principal
    ){
        return ResponseEntity.ok(userService.toggleUserPhoneVisibility(principal.getId()));
    }

    @PatchMapping("/preferences/student-section-visibility/toggle")
    @Operation(summary = "Toggle the visibility of user's student section in frontend")
    public ResponseEntity<UserPreferenceUpdateResponse> toggleUserStudentSectionVisibility(
            @AuthenticationPrincipal CustomUserDetails principal
    ){
        return ResponseEntity.ok(userService.toggleUserStudentSectionVisibility(principal.getId()));
    }

    @PatchMapping("/preferences/teacher-section-visibility/toggle")
    @Operation(summary = "Toggle the visibility of user's teacher section in frontend")
    public ResponseEntity<UserPreferenceUpdateResponse> toggleUserTeacherSectionVisibility(
            @AuthenticationPrincipal CustomUserDetails principal
    ){
        return ResponseEntity.ok(userService.toggleUserTeacherSectionVisibility(principal.getId()));
    }

    @PatchMapping("/avatar")
    @Operation(summary = "Upload a new avatar(profile picture)")
    public ResponseEntity<AvatarUploadResponse> uploadAvatar(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("file") MultipartFile file
    ){
        return ResponseEntity.ok(userService.uploadAvatar(principal.getId(), file));
    }

    // --- Delete Mapping ---
    @DeleteMapping("/avatar")
    @Operation(summary = "Delete the avatar(profile picture)")
    public ResponseEntity<Void> deleteAvatar(
            @AuthenticationPrincipal CustomUserDetails principal
    ){
        userService.deleteUserAvatar(principal.getId());
        return ResponseEntity.noContent().build();
    }
}
