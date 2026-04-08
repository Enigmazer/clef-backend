package com.enigmazer.clef.dto.topic;

import java.time.Instant;

public record TopicResponse(
        Long id,
        String title,
        Integer orderIndex,
        Instant completedAt
){}