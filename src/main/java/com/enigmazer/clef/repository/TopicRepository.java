package com.enigmazer.clef.repository;

import com.enigmazer.clef.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    @Query("SELECT COALESCE(MAX(t.orderIndex), 0) " +
            "FROM Topic t WHERE t.unit.id = :unitId")
    int findMaxOrderIndex(@Param("unitId") Long unitId);

    @Query("SELECT t FROM Topic t WHERE t.id = :id AND " +
            "t.unit.id = :unitId AND t.unit.subject.id = :subjectId")
    Optional<Topic> findByIdAndUnitIdAndSubjectId(
            @Param("id") Long id,
            @Param("unitId") Long unitId,
            @Param("subjectId") Long subjectId
    );

    @Query("SELECT t FROM Topic t WHERE t.completedAt IS NULL AND " +
            "(:currentTopicId IS NULL OR t.id != :currentTopicId) AND t.orderIndex = " +
            ":orderIndex AND t.unit.id = :unitId AND t.unit.subject.id = :subjectId")
    Optional<Topic> findByOrderIndexAndUnitIdAndSubjectId(
            @Param("orderIndex") int orderIndex,
            @Param("unitId") Long unitId,
            @Param("subjectId") Long subjectId,
            @Param("currentTopicId") Long currentTopicId
    );

    @Query("SELECT t FROM Topic t WHERE t.completedAt IS NULL AND " +
            "(:currentTopicId IS NULL OR t.id != :currentTopicId) AND t.unit.orderIndex = " +
            ":unitOrderIndex AND t.unit.subject.id = :subjectId ORDER BY t.orderIndex ASC LIMIT 1")
    Optional<Topic> findFirstTopicByUnitOrderIndexAndSubjectId(
            @Param("unitOrderIndex") int unitOrderIndex,
            @Param("subjectId") Long subjectId,
            @Param("currentTopicId") Long currentTopicId
    );

    @Query("SELECT t FROM Topic t WHERE t.completedAt IS NULL AND " +
            "(:currentTopicId IS NULL OR t.id != :currentTopicId) AND t.unit.subject.id " +
            "= :subjectId ORDER BY t.unit.orderIndex ASC, t.orderIndex ASC LIMIT 1")
    Optional<Topic> findRemainingFirstTopicBySubjectId(
            @Param("subjectId") Long subjectId,
            @Param("currentTopicId") Long currentTopicId
    );
}