package com.vigilai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/** Join table: who belongs to which workspace, and with what role. */
@Entity
@Table(name = "workspace_member", uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private WorkspaceRole role = WorkspaceRole.MEMBER;

    @Column(name = "joined_at", updatable = false)
    private Instant joinedAt;

    @PrePersist
    void onCreate() {
        joinedAt = Instant.now();
    }
}