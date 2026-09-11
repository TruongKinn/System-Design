package com.dataflow.export.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType; // EXPORT_JOB

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId; // jobId

    @Column(name = "event_type", nullable = false)
    private String eventType; // EXPORT_REQUESTED

    @Column(nullable = false, length = 4000)
    private String payload; // JSON representation of event

    @Column(nullable = false)
    private String status; // PENDING, PUBLISHED, FAILED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
