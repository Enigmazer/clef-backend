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
        name = "units",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_units_subject_order", columnNames = {"subject_id","order_index"}),
                @UniqueConstraint(
                        name = "uq_units_subject_title", columnNames = {"subject_id", "title"})
        }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Subject subject;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @OneToMany(mappedBy = "unit", fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private Set<Topic> topics = new LinkedHashSet<>();

    @CreationTimestamp
    @Column(name = "created_At", nullable = false, updatable = false)
    private Instant createdAt;
}
