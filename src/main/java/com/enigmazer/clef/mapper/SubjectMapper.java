package com.enigmazer.clef.mapper;

import com.enigmazer.clef.dto.subject.student.SubjectDetailsStudentResponse;
import com.enigmazer.clef.dto.subject.teacher.SubjectDetailsTeacherResponse;
import com.enigmazer.clef.dto.subject.teacher.SubjectUpdateResponse;
import com.enigmazer.clef.dto.subject.teacher.SubjectsSummaryTeacherResponse;
import com.enigmazer.clef.entity.Subject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",
        uses = {TopicMapper.class, UnitMapper.class, PhoneNumberMapper.class})
public interface SubjectMapper {

    @Mapping(source = "locked", target = "locked")
    @Mapping(source = "archived", target = "archived")
    @Mapping(target = "isSyllabusPdfAvailable", expression = "java(subject.getSyllabusKey() != null)")
    SubjectDetailsTeacherResponse toTeacherResponse(Subject subject);

    @Mapping(source = "locked", target = "locked")
    SubjectsSummaryTeacherResponse toSummaryResponse(Subject subject);

    @Mapping(source = "locked", target = "locked")
    @Mapping(source = "archived", target = "archived")
    @Mapping(target = "isSyllabusPdfAvailable", expression = "java(subject.getSyllabusKey() != null)")
    SubjectUpdateResponse toUpdateResponse(Subject subject);

    @Mapping(target = "isSyllabusPdfAvailable", expression = "java(subject.getSyllabusKey() != null)")
    SubjectDetailsStudentResponse toStudentResponse(Subject subject);
}