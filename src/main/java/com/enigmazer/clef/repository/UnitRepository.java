package com.enigmazer.clef.repository;

import com.enigmazer.clef.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {

    @Query("SELECT COALESCE(MAX(u.orderIndex), 0) " +
            "FROM Unit u WHERE u.subject.id = :subjectId")
    int findMaxOrderIndex(@Param("subjectId") Long subjectId);

    @Query("SELECT u FROM Unit u " +
            "LEFT JOIN FETCH u.topics " +
            "WHERE u.id = :unitId AND u.subject.id = :subjectId")
    Optional<Unit> findWithTopicsByIdAndSubjectId(
            @Param("unitId") Long unitId,
            @Param("subjectId") Long subjectId);

    @Query("SELECT u FROM Unit u " +
            "LEFT JOIN FETCH u.topics " +
            "WHERE u.subject.id = :subjectId")
    List<Unit> findWithTopicsBySubjectId(
            @Param("subjectId") Long subjectId);

    Optional<Unit> findByIdAndSubjectId(Long unitId, Long subjectId);

    boolean existsByTitleAndSubjectId(String trimmed, Long subjectId);
}
