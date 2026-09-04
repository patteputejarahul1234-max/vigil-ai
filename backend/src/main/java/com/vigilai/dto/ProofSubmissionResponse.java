package com.vigilai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProofSubmissionResponse {
    private Long id;
    private Long taskId;
    private boolean verified;
    private String aiReason;
    private String taskStatus;
    private Instant submittedAt;
}