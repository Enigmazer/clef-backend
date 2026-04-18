package com.enigmazer.clef.entity;

import com.enigmazer.clef.enums.TopicMaterialType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "topic_materials")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TopicMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "topic_material_key", columnDefinition = "TEXT", nullable = false)
    private String topicMaterialKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TopicMaterialType type;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
