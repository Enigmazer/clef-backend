package com.enigmazer.clef.dto.topic;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record TopicCompleteResponse(
        Long id,
        @Schema(description = "null = incomplete, timestamp = completed")
        Instant completedAt
){}
