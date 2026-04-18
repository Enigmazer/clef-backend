package com.enigmazer.clef.mapper;

import com.enigmazer.clef.dto.subject.*;
import com.enigmazer.clef.entity.Subject;
import com.enigmazer.clef.service.storage.StorageService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring",
        uses = {TopicMapper.class, UnitMapper.class, PhoneNumberMapper.class})
public abstract class SubjectMapper {

    @Autowired
    protected StorageService storageService;

    @Named("generateUrl")
    protected String getSyllabusUrl(String key){
        if (key == null ) return null;
        return storageService.generateSyllabusUrl(key);
    }

    public abstract Subject toEntity(SubjectCreationRequest request);

    @Mapping(source = "locked", target = "locked")
    @Mapping(source = "archived", target = "archived")
    @Mapping(target = "syllabusFileUrl", expression = "java(getSyllabusUrl(subject.getSyllabusKey()))")
    public abstract SubjectDetailsTeacherResponse toTeacherResponse(Subject subject);

    @Mapping(source = "locked", target = "locked")
    @Mapping(source = "syllabusKey", target = "syllabusFileUrl", qualifiedByName = "generateUrl")
    @Mapping(source = "archived", target = "archived")
    @Mapping(source = "teacher.fullName", target = "teacherName")
    @Mapping(source = "teacher.avatarUrl", target = "teacherAvatar")
    @Mapping(source = "teacher.phoneNumbers", target = "teacherPhoneNumbers")
    public abstract SubjectDetailsStudentResponse toStudentResponse(Subject subject);

    @Mapping(source = "locked", target = "locked")
    public abstract ListTeacherSubjectsResponse toSummaryResponse(Subject subject);

    public abstract SubjectUpdateReqRes toUpdateResponse(Subject subject);
}