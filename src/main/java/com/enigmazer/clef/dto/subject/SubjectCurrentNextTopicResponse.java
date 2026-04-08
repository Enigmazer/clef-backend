package com.enigmazer.clef.dto.subject;

import com.enigmazer.clef.dto.unit.UnitResponse;

import java.time.Instant;

public record SubjectCurrentNextTopicResponse(
        Long id,
        UnitResponse unit,
        String title,
        Integer orderIndex,
        Instant completedAt
){}