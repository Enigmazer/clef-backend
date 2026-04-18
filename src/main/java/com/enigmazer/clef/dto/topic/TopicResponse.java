package com.enigmazer.clef.dto.topic;

import com.enigmazer.clef.dto.topicMaterial.TopicMaterialResponse;

import java.time.Instant;
import java.util.List;

public record TopicResponse(
        Long id,
        String title,
        Integer orderIndex,
        Instant completedAt,
        List<TopicMaterialResponse> topicMaterials
){}