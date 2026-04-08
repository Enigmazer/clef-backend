package com.enigmazer.clef.dto.subject;

import com.enigmazer.clef.dto.phone.PhoneNumberStudentResponse;
import com.enigmazer.clef.dto.unit.UnitDetailResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record SubjectDetailsStudentResponse(

        Long id,

        String name,

        @Schema(description = "The description of the subject.")
        String description,

        @Schema(description = "The unique 6-character code students use to enroll.")
        String joinCode,

        @Schema(description = "URL to the syllabus file, if provided.")
        String syllabusFileUrl,

        @Schema(description = "All the units and there topics of this subject.")
        List<UnitDetailResponse> units,

        @Schema(description = "Current topic getting taught by the teacher.")
        SubjectCurrentNextTopicResponse currentTopic,

        @Schema(description = "The topic that will be taught Topic next by the teacher.")
        SubjectCurrentNextTopicResponse nextTopic,

        @Schema(description = "Indicates if new enrollments are locked by the teacher.")
        boolean locked,

        boolean archived,

        Instant createdAt,


        // teacher info
        @Schema(description = "Name of the teacher who teaches this subject.")
        String teacherName,

        String teacherAvatar,

        @Schema(description = "Phone number of the teacher. " +
                "null if number not added or showPhoneToStudents = false")
        List<PhoneNumberStudentResponse> teacherPhoneNumbers
) { }