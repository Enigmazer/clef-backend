package com.enigmazer.clef.repository;

import com.enigmazer.clef.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    boolean existsByJoinCode(String code);

    Optional<Subject> findByIdAndTeacherId(Long subjectId, Long teacherId);

    @Query("SELECT s FROM Subject s " +
            "LEFT JOIN FETCH s.currentTopic ct " +
            "LEFT JOIN FETCH ct.unit " +
            "LEFT JOIN FETCH s.nextTopic nt " +
            "LEFT JOIN FETCH nt.unit " +
            "WHERE s.id = :subjectId AND s.teacher.id = :teacherId")
    Optional<Subject> findByIdAndTeacherIdWithTopics(
            @Param("subjectId") Long subjectId,
            @Param("teacherId") Long teacherId
    );

    List<Subject> findAllByTeacherIdAndIsArchivedFalse(Long teacherId);

    List<Subject> findAllByTeacherIdAndIsArchivedTrue(Long teacherId);

    @Query("SELECT s FROM Subject s " +
            "LEFT JOIN FETCH s.units u " +
            "LEFT JOIN FETCH u.topics " +
            "WHERE s.id = :id AND s.teacher.id = :teacherId")
    Optional<Subject> findByIdForTeacher(
            @Param("id") Long subjectId,
            @Param("teacherId") Long teacherId
    );

    @Query("SELECT s FROM Subject s " +
            "JOIN FETCH s.teacher t " +
            "LEFT JOIN FETCH t.phoneNumbers " +
            "LEFT JOIN FETCH s.units u " +
            "LEFT JOIN FETCH u.topics " +
            "WHERE s.id = :id " +
            "AND s.isArchived = false " +
            "AND EXISTS ( " +
            "SELECT e FROM Enrollment e " +
            "WHERE e.subject = s " +
            "AND e.student.id = :studentId " +
            ")")
    Optional<Subject> findByIdForStudent(
            @Param("id") Long subjectId,
            @Param("studentId") Long studentId
    );

    boolean existsByIdAndTeacherId(Long subjectId, Long teacherId);

    Optional<Subject> findByJoinCode(String joinCode);

}
