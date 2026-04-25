package com.enigmazer.clef.dto.subject.teacher;

import java.time.Instant;

public record SubjectUpdateResponse (
        Long id,

        String name,

        String description,

        String joinCode,

        boolean isSyllabusPdfAvailable,

        SubjectCurrentNextTopicResponse currentTopic,

        SubjectCurrentNextTopicResponse nextTopic,

        boolean locked,

        boolean archived,

        Instant updatedAt
) {}
