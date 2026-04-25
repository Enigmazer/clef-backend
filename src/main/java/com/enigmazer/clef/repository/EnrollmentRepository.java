package com.enigmazer.clef.repository;

import com.enigmazer.clef.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    @Query("SELECT e FROM Enrollment e " +
            "JOIN FETCH e.student " +
            "WHERE e.subject.id = :subjectId " +
            "AND e.subject.teacher.id = :teacherId")
    List<Enrollment> findAllEnrolledStudentsBySubjectId(
            @Param("subjectId") Long subjectId,
            @Param("teacherId") Long teacherId
    );

    @Query("SELECT e FROM Enrollment e " +
            "JOIN FETCH e.subject s " +
            "JOIN FETCH s.teacher " +
            "WHERE e.student.id = :studentId " +
            "AND s.isArchived = false")
    List<Enrollment> findAllEnrolledSubjectsByStudentId(
            @Param("studentId") Long studentId
    );

    boolean existsByStudentIdAndSubjectId(Long studentId, Long subjectId);

    void deleteByStudentIdAndSubjectId(Long studentId, Long subjectId);
}
