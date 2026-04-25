package com.enigmazer.clef.dto.topicMaterial;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record TopicMaterialDeleteRequest(
        @NotEmpty(message = "At least one material ID is required.")
        Set<Long> topicMaterialIds
) {}