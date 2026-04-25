package com.enigmazer.clef.service.subject;

import com.enigmazer.clef.dto.subject.common.SubjectUpdatedAtResponse;
import com.enigmazer.clef.dto.subject.student.SubjectDetailsStudentResponse;
import com.enigmazer.clef.dto.subject.student.SubjectJoinRequest;
import com.enigmazer.clef.dto.subject.student.SubjectsSummaryStudentResponse;
import com.enigmazer.clef.dto.subject.teacher.*;
import com.enigmazer.clef.dto.user.TeacherProfileResponse;

import java.util.List;

public interface SubjectService {
    SubjectDetailsTeacherResponse createSubject(SubjectCreationRequest request, Long teacherId);

    List<SubjectsSummaryTeacherResponse> listTeacherSubjects(Long teacherId);

    List<SubjectsSummaryTeacherResponse> listArchivedTeacherSubjects(Long teacherId);

    SubjectDetailsTeacherResponse getTeacherSubjectDetails(Long subjectId, Long teacherId);

    SubjectUpdateResponse updateSubject(Long subjectId, SubjectUpdateRequest request, Long teacherId);

    SubjectUpdateResponse toggleSubjectLock(Long subjectId, Long teacherId);

    SubjectUpdateResponse toggleSubjectArchive(Long subjectId, Long teacherId);

    SubjectUpdateResponse setCurrentTopic(Long subjectId, SubjectCurrentNextTopicRequest request, Long teacherId);

    SubjectUpdateResponse setNextTopic(Long subjectId, SubjectCurrentNextTopicRequest request, Long teacherId);

    void deleteSubject(Long subjectId, Long teacherId);

    List<SubjectsSummaryStudentResponse> listStudentSubjects(Long studentId);

    SubjectDetailsStudentResponse getStudentSubjectDetails(Long subjectId, Long studentId);

    TeacherProfileResponse getTeacherProfile(Long subjectId, Long studentId);

    SubjectsSummaryStudentResponse joinSubject(SubjectJoinRequest request, Long studentId);

    SubjectUpdatedAtResponse getSubjectUpdatedAt(Long subjectId, Long userId);
}