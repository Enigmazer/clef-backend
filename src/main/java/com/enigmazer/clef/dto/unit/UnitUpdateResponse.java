package com.enigmazer.clef.dto.unit;

import com.enigmazer.clef.dto.topic.TopicResponse;

import java.util.List;

public record UnitUpdateResponse(
        Long id,
        String title,
        List<TopicResponse> topics
) {}
