package com.enigmazer.clef.dto.unit;

import com.enigmazer.clef.dto.topic.TopicResponse;

import java.util.List;

public record UnitDetailResponse(
        Long id,
        String title,
        Integer orderIndex,
        List<TopicResponse> topics
){}
