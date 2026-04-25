package com.enigmazer.clef.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "subjects",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_subject_teacher_name", columnNames = {"teacher_id", "name"})
        }
)
@Check(
        name = "chk_current_next_topic_differ",
        constraints = "current_topic_id IS NULL OR next_topic_id IS NULL OR current_topic_id != next_topic_id"
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "join_code", length = 10, unique = true, nullable = false)
    private String joinCode;

    @Column(name = "syllabus_key", columnDefinition = "TEXT")
    private String syllabusKey;

    @OneToMany(mappedBy = "subject", fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private Set<Unit> units = new LinkedHashSet<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_topic_id")
    private Topic currentTopic;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_topic_id")
    private Topic nextTopic;

    @Builder.Default
    @Column(name = "is_locked", nullable = false)
    private boolean isLocked = false;

    @Builder.Default
    @Column(name = "is_archived", nullable = false)
    private boolean isArchived = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
