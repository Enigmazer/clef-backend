package com.enigmazer.clef.dto.homework;

import java.time.Instant;
import java.util.Set;

public record HomeWorkUpdateRequest(

        String title,

        String description,

        Set<Long> topicIds,

        Instant dueDate
) {}
