package com.vigilai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;

@Data
public class TaskRequest {
    @NotBlank
    private String title;
    private String description;
    private String status;     // TODO, IN_PROGRESS, IN_REVIEW, DONE
    private String priority;   // LOW, MEDIUM, HIGH, URGENT
    private Long assigneeId;
    private Instant dueDate;
}
