package com.enigmazer.clef.dto.subject.student;

import jakarta.validation.constraints.NotBlank;

public record SubjectJoinRequest (
        @NotBlank(message = "Join code required.")
        String joinCode
) {}
