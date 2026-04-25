package com.enigmazer.clef.dto.topic;

import jakarta.validation.constraints.NotBlank;

public record TopicCreationUpdateRequest(
        @NotBlank
        String title
) {}
