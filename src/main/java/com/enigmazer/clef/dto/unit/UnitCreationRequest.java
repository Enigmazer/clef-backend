package com.enigmazer.clef.dto.unit;

import com.enigmazer.clef.dto.topic.TopicCreationUpdateRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UnitCreationRequest (

        @NotBlank
        String title,
        @NotNull
        List<TopicCreationUpdateRequest> topics
){}