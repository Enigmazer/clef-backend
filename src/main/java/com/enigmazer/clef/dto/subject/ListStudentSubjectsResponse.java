package com.enigmazer.clef.dto.subject;

import java.time.Instant;

public record ListStudentSubjectsResponse(
        Long id,
        String name,
        String description,
        String teacherName,
        Instant joinedAt
){}