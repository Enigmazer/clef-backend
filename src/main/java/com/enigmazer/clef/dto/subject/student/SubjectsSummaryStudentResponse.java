package com.enigmazer.clef.dto.subject.student;

import java.time.Instant;

public record SubjectsSummaryStudentResponse(
        Long id,
        String name,
        String description,
        String teacherName,
        String teacherAvatarUrl,
        Instant joinedAt,
        Instant updatedAt
){}