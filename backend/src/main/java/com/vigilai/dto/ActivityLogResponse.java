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
public class ActivityLogResponse {
    private Long id;
    private Long actorId;
    private String actorName;
    private String action;
    private String entityType;
    private Long entityId;
    private String details;
    private Instant createdAt;
}
