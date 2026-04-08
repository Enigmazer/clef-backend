package com.enigmazer.clef.mapper;

import com.enigmazer.clef.dto.subject.*;
import com.enigmazer.clef.entity.Subject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {TopicMapper.class, UnitMapper.class, PhoneNumberMapper.class})
public interface SubjectMapper {
    Subject toEntity(SubjectCreationRequest request);

    @Mapping(source = "locked", target = "locked")
    @Mapping(source = "archived", target = "archived")
    SubjectDetailsTeacherResponse toTeacherResponse(Subject subject);

    @Mapping(source = "locked", target = "locked")
    @Mapping(source = "archived", target = "archived")
    @Mapping(source = "teacher.fullName", target = "teacherName")
    @Mapping(source = "teacher.avatarUrl", target = "teacherAvatar")
    @Mapping(source = "teacher.phoneNumbers", target = "teacherPhoneNumbers")
    SubjectDetailsStudentResponse toStudentResponse(Subject subject);

    @Mapping(source = "locked", target = "locked")
    ListTeacherSubjectsResponse toSummaryResponse(Subject subject);

    SubjectUpdateReqRes toUpdateResponse(Subject subject);
}