package com.enigmazer.clef.service.enrollment;

import com.enigmazer.clef.dto.enrollment.EnrolledStudentResponse;
import com.enigmazer.clef.entity.Subject;
import com.enigmazer.clef.exception.InvalidRequestException;
import com.enigmazer.clef.exception.ResourceNotFoundException;
import com.enigmazer.clef.mapper.EnrollmentMapper;
import com.enigmazer.clef.repository.EnrollmentRepository;
import com.enigmazer.clef.repository.SubjectRepository;
import com.enigmazer.clef.service.common.SubjectHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentServiceImpl implements EnrollmentService{

    private final EnrollmentRepository enrollmentRepository;
    private final SubjectRepository subjectRepository;

    private final EnrollmentMapper enrollmentMapper;

    private final SubjectHelper subjectHelper;

    @Override
    public List<EnrolledStudentResponse> listEnrolledStudents(Long subjectId, Long teacherId) {
        if (!subjectRepository.existsByIdAndTeacherId(subjectId, teacherId)) {
            throw new ResourceNotFoundException("Subject not found");
        }

        List<EnrolledStudentResponse> enrolledStudentList = enrollmentRepository
                .findAllEnrolledStudentsBySubjectId(subjectId, teacherId)
                .stream()
                .map(enrollmentMapper::toResponse)
                .toList();

        log.info("Returned all students enrolled in the subject " +
                "[subjectId={}, teacherId={}]", subjectId, teacherId);
        return enrolledStudentList;
    }

    @Override
    @Transactional
    public void removeEnrollment(Long subjectId, Long studentId, Long teacherId) {
        Subject subject = subjectHelper.findSubjectByIdAndTeacherId(subjectId, teacherId);

        subjectHelper.checkArchived(subject);

        if (!enrollmentRepository.existsByStudentIdAndSubjectId(studentId, subjectId)) {
            throw new InvalidRequestException("Enrollment not found");
        }

        enrollmentRepository.deleteByStudentIdAndSubjectId(studentId, subjectId);
        log.info("Student enrollment successfully removed from subject [studentId={}, " +
                "subjectId={}, teacherId={}]", studentId, subjectId, teacherId);
    }

    @Override
    public void unEnrollment(Long subjectId, Long studentId) {
        if (!enrollmentRepository.existsByStudentIdAndSubjectId(studentId, subjectId)) {
            throw new InvalidRequestException("Enrollment not found");
        }

        enrollmentRepository.deleteByStudentIdAndSubjectId(studentId, subjectId);
        log.info("Student successfully unEnrollment himself from subject " +
                "[studentId={}, subjectId={}]", studentId, subjectId);
    }
}
