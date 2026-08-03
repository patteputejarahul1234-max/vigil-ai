package com.vigilai.dto;

import com.vigilai.entity.TaskPriority;
import com.vigilai.entity.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private Long assigneeId;
    private Long createdBy;
    private Instant dueDate;
    private int attachmentCount;
    private Instant createdAt;
    private Instant updatedAt;
}
