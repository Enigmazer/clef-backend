package com.enigmazer.clef.service.subject;

import com.enigmazer.clef.dto.enrollment.EnrolledStudentResponse;
import com.enigmazer.clef.dto.homework.HomeworkCreationRequest;
import com.enigmazer.clef.dto.homework.HomeworkResponse;
import com.enigmazer.clef.dto.subject.*;
import com.enigmazer.clef.dto.unit.UnitCreationRequest;
import com.enigmazer.clef.dto.unit.UnitParseResponse;
import com.enigmazer.clef.dto.user.TeacherProfileResponse;
import com.enigmazer.clef.service.page.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SubjectService {
    SubjectDetailsTeacherResponse createSubject(SubjectCreationRequest request, Long teacherId);

    HomeworkResponse createHomework(Long subjectId, HomeworkCreationRequest request, Long teacherId);

    List<ListTeacherSubjectsResponse> listTeacherSubjects(Long teacherId);

    List<ListTeacherSubjectsResponse> listArchivedTeacherSubjects(Long teacherId);

    List<ListStudentSubjectsResponse> listStudentSubjects(Long studentId);

    PageResponse<HomeworkResponse> getHomeWorkPage(Long subjectId, String filter, int page, Long userId);

    List<EnrolledStudentResponse> listEnrolledStudents(Long subjectId, Long teacherId);

    List<UnitParseResponse> parseSyllabus(Long subjectId, Long teacherId);

    SubjectDetailsTeacherResponse getTeacherSubjectDetails(Long subjectId, Long teacherId);

    SubjectDetailsStudentResponse getStudentSubjectDetails(Long subjectId, Long studentId);

    TeacherProfileResponse getTeacherProfile(Long subjectId, Long studentId);

    String getSyllabusUrl(Long subjectId, Long userId);

    SubjectUpdateReqRes updateSubject(Long subjectId, SubjectUpdateReqRes request, Long teacherId);

    boolean toggleSubjectLock(Long subjectId, Long teacherId); // returns new lock state

    boolean toggleSubjectArchive(Long subjectId, Long teacherId); // returns new archive state

    String uploadSyllabus(Long subjectId, MultipartFile file, Long teacherId);

    SubjectCurrentNextTopicResponse setCurrentTopic(Long subjectId, SubjectCurrentNextTopicRequest request, Long teacherId);

    SubjectCurrentNextTopicResponse setNextTopic(Long subjectId, SubjectCurrentNextTopicRequest request, Long teacherId);

    void deleteSubject(Long subjectId, Long teacherId);

    void deleteSyllabus(Long subjectId, Long teacherId);

    SubjectDetailsStudentResponse joinSubject(String joinCode, Long studentId);

    SubjectDetailsTeacherResponse bulkAddUnits(Long subjectId, List<UnitCreationRequest> request, Long teacherId);

}