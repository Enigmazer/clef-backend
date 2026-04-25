package com.enigmazer.clef.service.Syllabus;

import com.enigmazer.clef.dto.subject.common.SubjectSyllabusUrlResponse;
import com.enigmazer.clef.dto.subject.teacher.SubjectUpdateResponse;
import com.enigmazer.clef.dto.unit.UnitParseResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SyllabusService {
    SubjectSyllabusUrlResponse getSyllabusUrl(Long subjectId, Long userId);

    SubjectUpdateResponse uploadSyllabus(Long subjectId, MultipartFile file, Long teacherId);

    SubjectUpdateResponse deleteSyllabus(Long subjectId, Long teacherId);

    List<UnitParseResponse> parseSyllabus(Long subjectId, Long teacherId);
}
