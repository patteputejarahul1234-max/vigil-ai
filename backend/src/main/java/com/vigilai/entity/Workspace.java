package com.vigilai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workspaces")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.userId == null && this.ownerId != null) {
            this.userId = this.ownerId;
        } else if (this.ownerId == null && this.userId != null) {
            this.ownerId = this.userId;
        }
    }
}