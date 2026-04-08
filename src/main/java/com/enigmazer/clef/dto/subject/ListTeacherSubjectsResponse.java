package com.enigmazer.clef.dto.subject;

import java.time.Instant;

public record ListTeacherSubjectsResponse(
        Long id,
        String name,
        String description,
        String joinCode,
        boolean locked,
        Instant createdAt
) {}