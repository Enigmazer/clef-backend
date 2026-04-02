package com.enigmazer.clef.dto.user;

import java.time.Instant;

public record UserResponse(
    Long id,
    String role,
    String email,
    String fullName,
    String avatarUrl,
    boolean hasPassword,
    boolean twoFactorEnabled,
    boolean showPhoneToStudents,
    boolean hideStudentSection,
    boolean hideTeacherSection,
    Instant createdAt
) {}
