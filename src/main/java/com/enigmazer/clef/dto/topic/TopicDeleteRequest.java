package com.enigmazer.clef.dto.topic;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record TopicDeleteRequest(
        @NotEmpty(message = "At least one topic ID is required.")
        Set<Long> topicIds
) {}
