package com.enigmazer.clef.dto.homework;

import jakarta.validation.constraints.FutureOrPresent;

import java.time.Instant;
import java.util.Set;

public record HomeWorkUpdateRequest(

        String title,

        String description,

        Set<Long> topicIds,

        @FutureOrPresent(message = "You can't create a homework in past")
        Instant dueDate
) {}
