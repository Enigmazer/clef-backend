package com.enigmazer.clef.repository;

import com.enigmazer.clef.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    @Query("SELECT COALESCE(MAX(t.orderIndex), 0) " +
            "FROM Topic t WHERE t.unit.id = :unitId")
    int findMaxOrderIndex(@Param("unitId") Long unitId);

    @Query("SELECT t FROM Topic t WHERE t.id = :id AND " +
            "t.unit.id = :unitId AND t.unit.subject.id = :subjectId")
    Optional<Topic> findByIdAndParentValidation(
            @Param("id") Long id,
            @Param("unitId") Long unitId,
            @Param("subjectId") Long subjectId
    );

    @Query("SELECT t FROM Topic t " +
            "LEFT JOIN FETCH t.topicMaterials " +
            "WHERE t.id IN (:ids) AND " +
            "t.unit.id = :unitId AND t.unit.subject.id = :subjectId")
    List<Topic> findWithTopicMaterialsByIdsAndParentValidation(
            @Param("ids") Set<Long> ids,
            @Param("unitId") Long unitId,
            @Param("subjectId") Long subjectId
    );

    @Query("SELECT t FROM Topic t " +
            "WHERE t.id IN (:ids) AND " +
            "t.unit.subject.id = :subjectId")
    Set<Topic> findByIdsAndSubjectId(
            @Param("ids") Set<Long> ids,
            @Param("subjectId") Long subjectId
    );

    @Query("SELECT t FROM Topic t WHERE t.completedAt IS NULL AND " +
            "(:currentTopicId IS NULL OR t.id != :currentTopicId) AND t.orderIndex = " +
            ":orderIndex AND t.unit.id = :unitId AND t.unit.subject.id = :subjectId")
    Optional<Topic> findByOrderIndexAndParentValidation(
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

    boolean existsByTitleAndUnitId(String trimmed, Long unitId);
}