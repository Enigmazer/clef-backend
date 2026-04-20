package com.enigmazer.clef.dto.subject;

import com.enigmazer.clef.dto.unit.UnitDetailResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "The comprehensive data returned when fetching a subject.")
public record SubjectDetailsTeacherResponse(

        Long id,

        String name,

        @Schema(description = "The description of the subject.")
        String description,

        @Schema(description = "The unique 6-character code students use to enroll.")
        String joinCode,

        @Schema(description = "All the units and there topics of this subject.")
        List<UnitDetailResponse> units,

        @Schema(description = "Current topic getting taught by the teacher.")
        SubjectCurrentNextTopicResponse currentTopic,

        @Schema(description = "The topic that will be taught Topic next by the teacher.")
        SubjectCurrentNextTopicResponse nextTopic,

        boolean isSyllabusPdfAvailable,

        @Schema(description = "Indicates if new enrollments are locked by the teacher.")
        boolean locked,

        boolean archived,

        Instant createdAt
) {}
