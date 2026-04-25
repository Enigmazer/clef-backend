package com.enigmazer.clef.dto.subject.teacher;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for initializing a new academic subject. " +
        "This establishes the root container for curriculum units, topics, and student enrollments.")
public record SubjectCreationRequest (
    @Schema(
            description = "Name of the subject.",
            example = "Advanced Java Programming",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Subject name is required.")
    @Size(max = 255, message = "Subject name cannot exceed 255 characters.")
    String name,

    @Schema(
            description = "A detailed overview of what the subject covers.",
            example = "An in-depth look at Spring Boot, JPA, and concurrency."
    )
    String description
){}