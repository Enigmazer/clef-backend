package com.enigmazer.clef.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "topics",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_topics_unit_order", columnNames = {"unit_id", "order_index"}),
                @UniqueConstraint(
                        name = "uq_topics_unit_title", columnNames = {"unit_id", "title"})
        }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Topic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Unit unit;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @OneToMany(mappedBy = "topic", fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private Set<TopicMaterial> topicMaterials = new LinkedHashSet<>();

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
