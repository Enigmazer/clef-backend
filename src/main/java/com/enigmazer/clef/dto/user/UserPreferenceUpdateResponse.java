package com.enigmazer.clef.dto.user;

public record UserPreferenceUpdateResponse(
        boolean showPhoneToStudents,
        boolean hideStudentSection,
        boolean hideTeacherSection
) {}