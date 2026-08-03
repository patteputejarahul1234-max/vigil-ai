package com.vigilai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Append-only audit trail: "who did what, to which entity, when" per workspace. */
@Entity
@Table(name = "activity_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(nullable = false)
    private Long actorId;

    @Column(nullable = false)
    private String action;         // e.g. "TASK_CREATED", "TASK_STATUS_CHANGED"

    @Column(nullable = false)
    private String entityType;     // e.g. "TASK", "PROJECT", "WORKSPACE"

    @Column(nullable = false)
    private Long entityId;

    private String details;        // short human-readable summary

    @Column(updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
