package com.vigilai.dto;

import com.vigilai.entity.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private Long id;
    private Long workspaceId;
    private String name;
    private String description;
    private ProjectStatus status;
    private Long createdBy;
    private int taskCount;
    private int completedTaskCount;
    private Instant createdAt;
    private Instant updatedAt;
}
