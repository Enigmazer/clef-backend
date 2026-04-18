package com.enigmazer.clef.repository;

import com.enigmazer.clef.entity.TopicMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


public interface TopicMaterialRepository extends JpaRepository<TopicMaterial, Long> {

    @Transactional(readOnly = true)
    @Query("SELECT tm FROM TopicMaterial tm " +
            "WHERE tm.id IN (:ids) " +
            "AND tm.topic.id = :topicId " +
            "AND tm.topic.unit.id = :unitId " +
            "AND tm.topic.unit.subject.id = :subjectId"
    )
    List<TopicMaterial> findByIdsWithParentValidation(
            @Param("ids") List<Long> topicMaterialIds,
            @Param("topicId") Long topicId,
            @Param("unitId") Long unitId,
            @Param("subjectId") Long subjectId
    );

    @Transactional(readOnly = true)
    @Query("SELECT tm FROM TopicMaterial tm " +
            "WHERE tm.id = :id " +
            "AND tm.topic.id = :topicId " +
            "AND tm.topic.unit.id = :unitId " +
            "AND tm.topic.unit.subject.id = :subjectId " +
            "AND ( tm.topic.unit.subject.teacher.id = :userId " +
            "OR EXISTS (" +
            "SELECT e FROM Enrollment e " +
            "WHERE e.student.id = :userId " +
            "AND e.subject.id = :subjectId " +
            "AND e.subject.isArchived = false) )"
    )
    Optional<TopicMaterial> findByIdIfAccessible(
            @Param("id") Long topicMaterialId,
            @Param("topicId") Long topicId,
            @Param("unitId") Long unitId,
            @Param("subjectId") Long subjectId,
            @Param("userId") Long userId
    );
}