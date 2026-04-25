package com.enigmazer.clef.service.enrollment;

import com.enigmazer.clef.dto.enrollment.EnrolledStudentResponse;

import java.util.List;

public interface EnrollmentService {
    List<EnrolledStudentResponse> listEnrolledStudents(Long subjectId, Long teacherId);

    void removeEnrollment(Long subjectId, Long studentId, Long teacherId);

    void unEnrollment(Long subjectId, Long studentId);
}
