package com.enigmazer.clef.service.subject;

import com.enigmazer.clef.dto.enrollment.EnrolledStudentResponse;
import com.enigmazer.clef.dto.subject.*;
import com.enigmazer.clef.dto.subject.SubjectCurrentNextTopicRequest;
import com.enigmazer.clef.dto.subject.SubjectCurrentNextTopicResponse;
import com.enigmazer.clef.dto.unit.UnitCreationRequest;

import java.util.List;

public interface SubjectService {
    SubjectDetailsTeacherResponse createSubject(SubjectCreationRequest request, Long teacherId);

    List<ListTeacherSubjectsResponse> listTeacherSubjects(Long teacherId);

    List<ListTeacherSubjectsResponse> listArchivedTeacherSubjects(Long teacherId);

    List<ListStudentSubjectsResponse> listStudentSubjects(Long studentId);

    List<EnrolledStudentResponse> listEnrolledStudents(Long subjectId, Long teacherId);

    SubjectDetailsTeacherResponse getTeacherSubjectDetails(Long subjectId, Long teacherId);

    SubjectDetailsStudentResponse getStudentSubjectDetails(Long subjectId, Long studentId);

    SubjectUpdateReqRes updateSubject(Long subjectId, SubjectUpdateReqRes request, Long teacherId);
    
    boolean toggleSubjectLock(Long subjectId, Long teacherId); // returns new lock state

    boolean toggleSubjectArchive(Long subjectId, Long teacherId); // returns new archive state

    String setSyllabusUrl(Long subjectId, String syllabusUrl, Long teacherId);

    SubjectCurrentNextTopicResponse setCurrentTopic(Long subjectId, SubjectCurrentNextTopicRequest request, Long teacherId);

    SubjectCurrentNextTopicResponse setNextTopic(Long subjectId, SubjectCurrentNextTopicRequest request, Long teacherId);

    void deleteSubject(Long subjectId, Long teacherId);

    SubjectDetailsStudentResponse joinSubject(String joinCode, Long studentId);

    SubjectDetailsTeacherResponse bulkAddUnits(Long subjectId, List<UnitCreationRequest> request, Long teacherId);
}