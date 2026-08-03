package com.vigilai.dto;

import lombok.Data;

import java.time.Instant;

/** All fields optional — used to build a dynamic filter for GET /api/tasks/search */
@Data
public class TaskSearchRequest {
    private Long projectId;
    private Long workspaceId;
    private String status;
    private String priority;
    private Long assigneeId;
    private String keyword;      // matches title/description
    private Instant dueBefore;
    private Instant dueAfter;
}
