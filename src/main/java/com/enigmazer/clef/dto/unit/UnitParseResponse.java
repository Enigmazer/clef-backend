package com.enigmazer.clef.dto.unit;

import com.enigmazer.clef.dto.topic.TopicParseResponse;

import java.util.List;

public record UnitParseResponse (
    String title,
    List<TopicParseResponse> topics
){}
