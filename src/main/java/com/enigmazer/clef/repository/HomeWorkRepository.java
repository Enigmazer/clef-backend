package com.enigmazer.clef.repository;

import com.enigmazer.clef.entity.Homework;
import com.enigmazer.clef.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface HomeWorkRepository extends JpaRepository<Homework, Long> {

    @Query("SELECT h.id FROM Homework h " +
            "WHERE h.subject = :subject " +
            "AND h.dueDate >= :now")
    Page<Long> findUpcomingHomeworkIdsBySubject(
            @Param("subject") Subject subject,
            @Param("now") Instant now,
            Pageable pageable);

    @Query(
            value = "SELECT h.id FROM Homework h " +
                    "WHERE h.subject = :subject " +
                    "AND h.dueDate < :now"
    )
    Page<Long> findPastHomeworkIdsBySubject(
            @Param("subject") Subject subject,
            @Param("now") Instant now,
            Pageable pageable);

    @Query("SELECT h FROM Homework h " +
            "LEFT JOIN FETCH h.topics " +
            "WHERE h.id IN :ids " +
            "ORDER BY h.dueDate ASC")
    List<Homework> findFutureHomeworksWithTopicsByIds(
            @Param("ids") List<Long> ids);


    @Query("SELECT h FROM Homework h " +
            "LEFT JOIN FETCH h.topics " +
            "WHERE h.id IN :ids " +
            "ORDER BY h.dueDate DESC")
    List<Homework> findPastHomeworksWithTopicsByIds(
            @Param("ids") List<Long> ids);

    @Query("SELECT h FROM Homework h " +
            "WHERE h.id = :homeWorkId " +
            "AND h.subject.id = :subjectId "
    )
    Optional<Homework> findByIdAndSubjectId(
            @Param("homeWorkId") Long homeWorkId,
            @Param("subjectId") Long subjectId);
}
