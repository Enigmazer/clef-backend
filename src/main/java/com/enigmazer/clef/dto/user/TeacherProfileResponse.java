package com.enigmazer.clef.dto.user;

import com.enigmazer.clef.dto.phone.PhoneNumberStudentResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record TeacherProfileResponse (
        String fullName,

        String avatarUrl,

        @Schema(description = "Phone number of the teacher. " +
                "null if number not added or private")
        List<PhoneNumberStudentResponse> phoneNumbers
){}
