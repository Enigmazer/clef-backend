package com.enigmazer.clef.repository;

import com.enigmazer.clef.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    boolean existsByJoinCode(String code);

    Optional<Subject> findByIdAndTeacherId(Long subjectId, Long teacherId);

    @Transactional(readOnly = true)
    @Query("SELECT s FROM Subject s " +
            "LEFT JOIN FETCH s.currentTopic ct " +
            "LEFT JOIN FETCH ct.unit " +
            "LEFT JOIN FETCH s.nextTopic nt " +
            "LEFT JOIN FETCH nt.unit " +
            "WHERE s.id = :subjectId AND s.teacher.id = :teacherId")
    Optional<Subject> findWithCurrentAndNextTopicByIdAndTeacherId(
            @Param("subjectId") Long subjectId,
            @Param("teacherId") Long teacherId
    );

    @Transactional(readOnly = true)
    @Query("SELECT s FROM Subject s " +
            "LEFT JOIN FETCH s.units u " +
            "LEFT JOIN FETCH u.topics t " +
            "LEFT JOIN FETCH t.topicMaterials tm " +
            "WHERE s.id = :subjectId AND s.teacher.id = :teacherId"
    )
    Optional<Subject> findWithTopicMaterialsByIdAndTeacherId(
            @Param("subjectId") Long subjectId,
            @Param("teacherId") Long teacherId
    );

    List<Subject> findAllByTeacherIdAndIsArchivedFalse(Long teacherId);

    List<Subject> findAllByTeacherIdAndIsArchivedTrue(Long teacherId);

    @Transactional(readOnly = true)
    @Query("SELECT s FROM Subject s " +
            "LEFT JOIN FETCH s.units u " +
            "LEFT JOIN FETCH u.topics t " +
            "LEFT JOIN FETCH t.topicMaterials " +
            "WHERE s.id = :id AND s.teacher.id = :teacherId")
    Optional<Subject> findSubjectDetailByIdAndTeacherId(
            @Param("id") Long subjectId,
            @Param("teacherId") Long teacherId
    );

    @Transactional(readOnly = true)
    @Query("SELECT s FROM Subject s " +
            "JOIN FETCH s.teacher t " +
            "LEFT JOIN FETCH t.phoneNumbers " +
            "LEFT JOIN FETCH s.units u " +
            "LEFT JOIN FETCH u.topics tp " +
            "LEFT JOIN FETCH tp.topicMaterials " +
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

    boolean existsByIdAndTeacherId(Long subjectId, Long teacherId);

    Optional<Subject> findByJoinCode(String joinCode);
}
