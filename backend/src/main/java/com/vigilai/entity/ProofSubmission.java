package com.vigilai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Records the OUTCOME of a proof-of-execution check, never the photo
 * itself. The image is verified in memory and discarded immediately —
 * this is the only trace that a submission happened.
 */
@Entity
@Table(name = "proof_submission")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProofSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private Long submittedBy;

    @Column(nullable = false)
    private boolean verified;

    @Column(length = 500)
    private String aiReason;

    @Column(updatable = false)
    private Instant submittedAt;

    @PrePersist
    void onCreate() {
        submittedAt = Instant.now();
    }
}