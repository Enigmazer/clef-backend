package com.enigmazer.clef.dto.subject.student;

import com.enigmazer.clef.dto.subject.teacher.SubjectCurrentNextTopicResponse;
import com.enigmazer.clef.dto.unit.UnitDetailResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record SubjectDetailsStudentResponse(

        Long id,

        String name,

        @Schema(description = "The description of the subject.")
        String description,

        @Schema(description = "All the units and there topics of this subject.")
        List<UnitDetailResponse> units,

        @Schema(description = "Current topic getting taught by the teacher.")
        SubjectCurrentNextTopicResponse currentTopic,

        @Schema(description = "The topic that will be taught Topic next by the teacher.")
        SubjectCurrentNextTopicResponse nextTopic,

        boolean isSyllabusPdfAvailable,

        Instant updatedAt
) {}