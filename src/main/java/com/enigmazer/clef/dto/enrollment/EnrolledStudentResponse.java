package com.enigmazer.clef.dto.enrollment;

import java.time.Instant;

public record EnrolledStudentResponse(
        Long id,
        String fullName,
        String avatarUrl,
        Instant joinedAt
) {}
