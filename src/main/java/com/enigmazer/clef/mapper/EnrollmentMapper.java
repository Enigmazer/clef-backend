package com.enigmazer.clef.mapper;

import com.enigmazer.clef.dto.enrollment.EnrolledStudentResponse;
import com.enigmazer.clef.dto.subject.ListStudentSubjectsResponse;
import com.enigmazer.clef.entity.Enrollment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {

    @Mapping(source = "student.id", target = "id")
    @Mapping(source = "student.fullName", target = "fullName")
    @Mapping(source = "student.avatarUrl", target = "avatarUrl")
    @Mapping(source = "joinedAt", target = "joinedAt")
    EnrolledStudentResponse toResponse(Enrollment enrollment);

    @Mapping(source = "subject.id", target = "id")
    @Mapping(source = "subject.name", target = "name")
    @Mapping(source = "subject.description", target = "description")
    @Mapping(source = "subject.teacher.fullName", target = "teacherName")
    @Mapping(source = "subject.teacher.avatarUrl", target = "teacherAvatarUrl")
    @Mapping(source = "joinedAt", target = "joinedAt")
    ListStudentSubjectsResponse toStudentSummary(Enrollment enrollment);
}
