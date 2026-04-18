package com.enigmazer.clef.repository;

import com.enigmazer.clef.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    @Transactional(readOnly = true)
    @Query("SELECT e FROM Enrollment e " +
            "JOIN FETCH e.student " +
            "WHERE e.subject.id = :subjectId " +
            "AND e.subject.teacher.id = :teacherId")
    List<Enrollment> findEnrollmentsBySubjectId(
            @Param("subjectId") Long subjectId,
            @Param("teacherId") Long teacherId
    );

    @Transactional(readOnly = true)
    @Query("SELECT e FROM Enrollment e " +
            "JOIN FETCH e.subject s " +
            "JOIN FETCH s.teacher " +
            "WHERE e.student.id = :studentId " +
            "AND s.isArchived = false")
    List<Enrollment> findAllStudentEnrollments(
            @Param("studentId") Long studentId
    );

    boolean existsByStudentIdAndSubjectId(Long studentId, Long subjectId);
}
