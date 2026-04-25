package com.enigmazer.clef.repository;

import com.enigmazer.clef.entity.Subject;
import com.enigmazer.clef.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    @Query("SELECT s FROM Subject s " +
            "LEFT JOIN FETCH s.currentTopic ct " +
            "LEFT JOIN FETCH ct.unit " +
            "LEFT JOIN FETCH s.nextTopic nt " +
            "LEFT JOIN FETCH nt.unit " +
            "WHERE s.id = :id AND s.teacher.id = :teacherId")
    Optional<Subject> findWithCurrentNextTopicsByIdAndTeacherId(
            @Param("id") Long subjectId,
            @Param("teacherId") Long teacherId
    );

    @Query("SELECT s FROM Subject s " +
            "LEFT JOIN FETCH s.units u " +
            "LEFT JOIN FETCH u.topics t " +
            "LEFT JOIN FETCH t.topicMaterials tm " +
            "WHERE s.id = :id AND s.teacher.id = :teacherId"
    )
    Optional<Subject> findSubjectDetailByIdAndTeacherId(
            @Param("id") Long subjectId,
            @Param("teacherId") Long teacherId
    );

    @Query("SELECT s FROM Subject s " +
            "LEFT JOIN FETCH s.units u " +
            "LEFT JOIN FETCH u.topics t " +
            "LEFT JOIN FETCH t.topicMaterials " +
            "WHERE s.id = :id " +
            "AND s.isArchived = false " +
            "AND EXISTS ( " +
            "SELECT e FROM Enrollment e " +
            "WHERE e.subject = s " +
            "AND e.student.id = :studentId " +
            ")")
    Optional<Subject> findSubjectDetailByIdAndStudentId(
            @Param("id") Long subjectId,
            @Param("studentId") Long studentId
    );

    @Query("SELECT t FROM Subject s " +
            "JOIN s.teacher t " +
            "LEFT JOIN FETCH t.phoneNumbers " +
            "WHERE s.id = :id " +
            "AND s.isArchived = false " +
            "AND EXISTS ( " +
            "SELECT e FROM Enrollment e " +
            "WHERE e.subject = s " +
            "AND e.student.id = :studentId " +
            ")")
    Optional<User> findTeacherByIdAndStudentId(
            @Param("id") Long subjectId,
            @Param("studentId") Long studentId
    );

    @Query("SELECT s FROM Subject s " +
            "WHERE s.id = :id " +
            "AND (s.teacher.id = :userId " +
            "OR (s.isArchived = false " +
            "AND EXISTS ( " +
            "SELECT e FROM Enrollment e " +
            "WHERE e.subject = s " +
            "AND e.student.id = :userId " +
            ")))")
    Optional<Subject> findSubjectByIdAndUserId(
            @Param("id") Long subjectId,
            @Param("userId") Long userId
    );

    Optional<Subject> findByJoinCode(String joinCode);

    Optional<Subject> findByIdAndTeacherId(Long subjectId, Long teacherId);

    List<Subject> findAllByTeacherIdAndIsArchivedFalse(Long teacherId);

    List<Subject> findAllByTeacherIdAndIsArchivedTrue(Long teacherId);

    boolean existsByJoinCode(String code);

    boolean existsByIdAndTeacherId(Long subjectId, Long teacherId);

    boolean existsByNameAndTeacherId(String trimmed, Long teacherId);
}
