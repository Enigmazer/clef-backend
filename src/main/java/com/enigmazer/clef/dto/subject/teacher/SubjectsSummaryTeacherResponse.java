package com.enigmazer.clef.dto.subject.teacher;

import java.time.Instant;

public record SubjectsSummaryTeacherResponse(
        Long id,
        String name,
        String description,
        String joinCode,
        boolean locked,
        Instant createdAt,
        Instant updatedAt
) {}