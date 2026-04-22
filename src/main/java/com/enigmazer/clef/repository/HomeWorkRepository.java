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
import java.util.Optional;

@Repository
public interface HomeWorkRepository extends JpaRepository<Homework, Long> {
    @Query(
            value = "SELECT h FROM Homework h " +
                    "LEFT JOIN FETCH h.topics " +
                    "WHERE h.subject = :subject " +
                    "AND h.dueDate >= :now",
            countQuery = "SELECT COUNT(h) FROM Homework h " +
                    "WHERE h.subject = :subject " +
                    "AND h.dueDate >= :now"
    )
    Page<Homework> findUpcomingHomeworkBySubject(
            @Param("subject") Subject subject,
            @Param("now") Instant now,
            Pageable pageable);

    @Query(
            value = "SELECT h FROM Homework h " +
                    "LEFT JOIN FETCH h.topics " +
                    "WHERE h.subject = :subject " +
                    "AND h.dueDate < :now",
            countQuery = "SELECT COUNT(h) FROM Homework h " +
                    "WHERE h.subject = :subject " +
                    "AND h.dueDate < :now"
    )
    Page<Homework> findPastHomeworkBySubject(
            @Param("subject") Subject subject,
            @Param("now") Instant now,
            Pageable pageable);

    @Query("SELECT h FROM Homework h " +
            "WHERE h.id = :homeWorkId " +
            "AND h.subject.id = :subjectId "
    )
    Optional<Homework> findByIdAndSubjectId(
            @Param("homeWorkId") Long homeWorkId,
            @Param("subjectId") Long subjectId);
}
