package com.enigmazer.clef.dto.unit;

import com.enigmazer.clef.dto.topic.TopicReorderRequest;

import java.util.List;

public record UnitReorderRequest(
        Long id,
        Integer orderIndex,
        List<TopicReorderRequest> topics
) {}
