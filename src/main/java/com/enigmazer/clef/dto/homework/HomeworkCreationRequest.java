package com.enigmazer.clef.dto.homework;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.Set;

public record HomeworkCreationRequest(

        @NotBlank(message = "Homework title is required.")
        String title,

        String description,

        Set<Long> topicIds,

        @FutureOrPresent(message = "You can't create a homework in past")
        Instant dueDate
) {}
