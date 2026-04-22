package com.enigmazer.clef.dto.homework;

import com.enigmazer.clef.dto.topic.HomeworkTopicResponse;

import java.time.Instant;
import java.util.Set;

public record HomeworkResponse(
        Long id,
        String title,
        String description,
        Set<HomeworkTopicResponse> topics,
        Instant dueDate,
        Instant createdAt
) {}
